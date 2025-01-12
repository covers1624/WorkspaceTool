package net.covers1624.wstool.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.MavenDependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.component.Artifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

        // For the moment, we only extract the dependencies on the compile/runtime classpaths.
        // TODO we should evaluate if we actually need this in tree form, or if we can flatten it.
        SourceSetList sourceSets = projectData.getData(SourceSetList.class);
        if (sourceSets == null) throw new RuntimeException("SourceSets not extracted prior to Configurations.");

        LinkedList<String> candidates = new LinkedList<>();
        sourceSets.asMap().values().forEach(e -> {
            candidates.add(e.compileClasspathConfiguration);
            candidates.add(e.runtimeClasspathConfiguration);
        });

        ConfigurationContainer configurations = project.getConfigurations();
        while (!candidates.isEmpty()) {
            String configurationName = candidates.removeFirst();
            Configuration configuration = configurations.findByName(configurationName);
            if (configuration == null) {
                project.getLogger().error("Missing configuration: {}", configurationName);
                continue;
            }
            ConfigurationData data = getOrCreate(configuration.getName());

            configuration.getExtendsFrom().forEach(extendsFrom -> {
                data.extendsFrom.put(extendsFrom.getName(), getOrCreate(extendsFrom.getName()));
                // TODO what if different projects? is that possible?
                candidates.add(extendsFrom.getName());
            });

            data.transitive = configuration.isTransitive();

            Configuration copy = configuration.copy();
            copy.setCanBeResolved(true);
            extractDependencies(copy, data);
        }
    }

    private void extractDependencies(Configuration configuration, ConfigurationData data) {
        // Strip out any Dependencies we need to handle specially. Projects, SourceSets, etc.
        for (Iterator<Dependency> iterator = configuration.getDependencies().iterator(); iterator.hasNext(); ) {
            ConfigurationData.Dependency dep = consumeRawDependency(iterator.next());
            if (dep != null) {
                data.dependencies.add(dep);
                iterator.remove();
            }
        }

        ResolvedConfiguration resolved = configuration.getResolvedConfiguration();
        data.dependencies.addAll(buildMavenDependencies(resolved.getFirstLevelModuleDependencies()));
    }

    private @Nullable ConfigurationData.Dependency consumeRawDependency(Dependency dep) {
        if (dep instanceof ProjectDependency) {
            return processProjectDep((ProjectDependency) dep);
        }
        if (dep instanceof FileCollectionDependency) {
            return processFileColDep((FileCollectionDependency) dep);
        }
        return null;
    }

    private ConfigurationData.ProjectDependency processProjectDep(ProjectDependency projectDep) {
        Project project = projectDep.getDependencyProject();
        ProjectData projectData = requireNonNull(lookupCache.projects.get(project), "Project missing from lookup! " + project);
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
            // Let's just assume we'll only ever have one artifact per dependency for now.
            // If this turns out to be a problem, add unit tests!
            ResolvedArtifact artifact = ColUtils.only(dep.getModuleArtifacts());
            MavenNotation notation = new MavenNotation(
                    dep.getModuleGroup(),
                    dep.getModuleName(),
                    dep.getModuleVersion(),
                    artifact.getClassifier(),
                    artifact.getExtension()
            );
            MavenDependency dependency = new MavenDependency(notation);

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

            dependency.children.addAll(buildMavenDependencies(dep.getChildren()));
            builtDeps.add(dependency);
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
}
