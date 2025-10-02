package net.covers1624.wstool.gradle.databuild;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectBuilder;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.MavenDependency;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.component.Artifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 9/9/23.
 */
@SuppressWarnings ("NotNullFieldNotInitialized") // See comment bellow above fields.
public class ConfigurationDataBuilder implements ProjectBuilder {

    // Mutable state used during this execution
    // Guaranteed to be set prior to all other functions.
    private Project project;
    private LookupCache lookupCache;
    private ConfigurationList configurationsData;

    @Override
    public void buildProjectData(Project project, ProjectData projectData, LookupCache lookupCache) {
        this.project = project;
        this.lookupCache = lookupCache;
        configurationsData = projectData.putData(ConfigurationList.class, new ConfigurationList());

        SourceSetContainer sourceSetContainer = SourceSetDataBuilder.getSourceSetContainer(project);
        if (sourceSetContainer == null) return;

        // For the moment, we only extract the dependencies on the compile/runtime classpaths.
        // TODO we should evaluate if we actually need this in tree form, or if we can flatten it.
        SourceSetList sourceSets = projectData.getData(SourceSetList.class);
        if (sourceSets == null) throw new RuntimeException("SourceSets not extracted prior to Configurations.");

        ConfigurationContainer configurations = project.getConfigurations();
        List<Configuration> extraConfigurations = FastStream.of(lookupCache.additionalConfigurations.getOrDefault(project, Collections.emptyList()))
                .map(configurations::findByName)
                .filter(Objects::nonNull)
                .toList();

        Map<Configuration, List<Dependency>> toRemove = new HashMap<>();
        sourceSets.asMap().values().forEach(e -> {
            extractProjectDependencies(project, toRemove, configurations.getAt(e.compileClasspathConfiguration), getOrCreate(e.compileClasspathConfiguration));
            extractProjectDependencies(project, toRemove, configurations.getAt(e.runtimeClasspathConfiguration), getOrCreate(e.runtimeClasspathConfiguration));
        });

        for (Configuration extra : extraConfigurations) {
            extractProjectDependencies(project, toRemove, extra, getOrCreate(extra.getName()));
        }

        // We must first iterate, every source set, then remove after. As inherited dependencies will get nuked from their
        // original location otherwise.
        toRemove.forEach((config, deps) -> deps.forEach(config.getDependencies()::remove));

        // We must run this after we potentially modify the configuration above, Once we poke at it by iterating files, etc,
        // it becomes immutable.
        sourceSets.asMap().values().forEach(e -> {
            SourceSet ss = sourceSetContainer.getByName(e.name);
            extractDependencies(
                    ss.getCompileClasspath().minus(ss.getOutput()), // subtract the current source sets output from the classpath.
                    configurations.getAt(e.compileClasspathConfiguration),
                    getOrCreate(e.compileClasspathConfiguration)
            );
            extractDependencies(
                    ss.getRuntimeClasspath().minus(ss.getOutput()),  // subtract the current source sets output from the classpath.
                    configurations.getAt(e.runtimeClasspathConfiguration),
                    getOrCreate(e.runtimeClasspathConfiguration)
            );
        });

        for (Configuration extra : extraConfigurations) {
            extractDependencies(
                    project.files(),
                    extra,
                    getOrCreate(extra.getName())
            );
        }
    }

    private void extractProjectDependencies(Project project, Map<Configuration, List<Dependency>> toRemove, Configuration configuration, ConfigurationData data) {
        for (Configuration config : configuration.getHierarchy()) {
            for (Dependency dependency : config.getDependencies()) {
                ConfigurationData.Dependency dep = consumeRawDependency(project, dependency);
                if (dep != null) {
                    data.dependencies.add(dep);
                    toRemove.computeIfAbsent(config, e -> new ArrayList<>())
                            .add(dependency);
                }
            }
        }
    }

    private void extractDependencies(FileCollection classpath, Configuration configuration, ConfigurationData data) {
        // Process inter source set dependencies placed directly onto the classpath of another sourceset.
        // This is most commonly used by the `test` source set compile/runtime classpath, which contains the output directories
        // of the main source set.
        Set<SourceSetData> sourceSetDeps = new HashSet<>();
        for (File file : classpath) {
            SourceSetData dep = lookupCache.sourceSetOutputs.get(file);
            if (dep != null) {
                sourceSetDeps.add(dep);
            }
        }

        for (SourceSetData sourceSetDep : sourceSetDeps) {
            data.dependencies.add(new ConfigurationData.SourceSetDependency(sourceSetDep));
        }

        ResolvedConfiguration resolved = configuration.getResolvedConfiguration();
        data.dependencies.addAll(buildMavenDependencies(resolved.getFirstLevelModuleDependencies()));
    }

    private ConfigurationData.@Nullable Dependency consumeRawDependency(Project project, Dependency dep) {
        if (dep instanceof ProjectDependency) {
            return processProjectDep(project, (ProjectDependency) dep);
        }
        if (dep instanceof FileCollectionDependency) {
            return processFileColDep((FileCollectionDependency) dep);
        }
        return null;
    }

    private ConfigurationData.ProjectDependency processProjectDep(Project rootProject, ProjectDependency projectDep) {
        Project project = getDependencyProject(rootProject, projectDep);
        ProjectData projectData = requireNonNull(lookupCache.projects.get(project.getPath()), "Project missing from lookup! " + project);
        return new ConfigurationData.ProjectDependency(projectData);
    }

    private @Nullable ConfigurationData.Dependency processFileColDep(FileCollectionDependency fileColDep) {
        FileCollection fileCol = fileColDep.getFiles();
        if (fileCol instanceof SourceSetOutput) {
            return processSourceSetDep((SourceSetOutput) fileCol);
        }

        return null;
    }

    private ConfigurationData.SourceSetDependency processSourceSetDep(SourceSetOutput output) {
        SourceSetData sourceSetData = requireNonNull(lookupCache.sourceSets.get(output), "SourceSetOutput missing from lookup!");
        return new ConfigurationData.SourceSetDependency(sourceSetData);
    }

    private List<MavenDependency> buildMavenDependencies(Iterable<ResolvedDependency> dependencies) {
        DependencyHandler depHandler = project.getDependencies();

        List<MavenDependency> builtDeps = new LinkedList<>();
        for (ResolvedDependency dep : dependencies) {
            // We may get any range of module artifacts for this dependency.
            // Zero usually indicates that it's a pom only dependency.
            // One is the normal.
            // More than one for a bom/pom dependency, This has only been seen with LWJGL provided by NeoForge.
            // We Just resolve all possible into a list and handle it bellow.
            List<MavenDependency> nestedDeps = new ArrayList<>();
            for (ResolvedArtifact artifact : dep.getModuleArtifacts()) {
                MavenNotation notation = new MavenNotation(
                        dep.getModuleGroup(),
                        dep.getModuleName(),
                        dep.getModuleVersion(),
                        artifact != null ? artifact.getClassifier() : null,
                        artifact != null ? artifact.getExtension() != null ? artifact.getExtension() : "jar" : "jar"
                );
                MavenDependency dependency = new MavenDependency(notation);
                if (artifact != null) {
                    dependency.files.put("classes", artifact.getFile());
                    //noinspection UnstableApiUsage,unchecked
                    ArtifactResolutionResult result = depHandler.createArtifactResolutionQuery()
                            .forComponents(artifact.getId().getComponentIdentifier())
                            .withArtifacts(JvmLibrary.class, SourcesArtifact.class, JavadocArtifact.class)
                            .execute();
                    Set<ComponentArtifactsResult> results = result.getResolvedComponents();
                    if (!results.isEmpty()) {
                        ComponentArtifactsResult r = ColUtils.only(results);
                        File sources = getArtifactFile(r, SourcesArtifact.class);
                        File javadoc = getArtifactFile(r, JavadocArtifact.class);

                        if (sources != null) dependency.files.put("sources", sources);
                        if (javadoc != null) dependency.files.put("javadoc", javadoc);
                    }
                }
                nestedDeps.add(dependency);
            }
            // If we got exactly one, then it's our primary dependency.
            MavenDependency primaryDep;
            if (nestedDeps.size() == 1) {
                primaryDep = nestedDeps.get(0);
            } else {
                // If we got more than one or zero, we make a group dependency.
                primaryDep = new MavenDependency(new MavenNotation(
                        dep.getModuleGroup(),
                        dep.getModuleName(),
                        dep.getModuleVersion(),
                        null,
                        "pom" // Pom dep used here as group, Not entirely sure if this is maven spec compliant.
                ));
                primaryDep.children.addAll(nestedDeps);
            }

            primaryDep.children.addAll(buildMavenDependencies(dep.getChildren()));
            builtDeps.add(primaryDep);
        }
        return builtDeps;
    }

    private static @Nullable File getArtifactFile(ComponentArtifactsResult ar, Class<? extends Artifact> type) {
        Set<ArtifactResult> sa = ar.getArtifacts(type);
        if (sa.isEmpty()) return null;
        ArtifactResult a = ColUtils.only(sa);
        if (!(a instanceof ResolvedArtifactResult)) return null;
        return ((ResolvedArtifactResult) a).getFile();
    }

    private ConfigurationData getOrCreate(String name) {
        return configurationsData.computeIfAbsent(name, ConfigurationData::new);
    }

    private Project getDependencyProject(Project rootProject, ProjectDependency dep) {
        if (GradleVersion.current().compareTo(GradleVersion.version("8.11")) >= 0) {
            // Was added in Gradle 8.11 as a replacement for getDependencyProject.
            return rootProject.project(dep.getPath());
        }
        // ProjectDependency.getDependencyProject was removed in Gradle 9.
        return (Project) InvokerHelper.getProperty(dep, "dependencyProject");
    }
}
