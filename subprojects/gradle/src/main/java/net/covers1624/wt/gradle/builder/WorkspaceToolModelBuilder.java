/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle.builder;

import net.covers1624.gradlestuff.dependencies.ConfigurationVisitor;
import net.covers1624.gradlestuff.dependencies.ConfigurationWalker;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.gradle.data.*;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.gradle.model.impl.WorkspaceToolModelImpl;
import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.gradle.util.PluginResolver;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.ScalaSourceSet;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.covers1624.gradlestuff.dependencies.ConfigurationWalker.ResolveOptions.*;

/**
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (6)
public class WorkspaceToolModelBuilder extends AbstractModelBuilder<WorkspaceToolModel> {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceToolModelBuilder.class);

    public WorkspaceToolModelBuilder() {
        super(WorkspaceToolModel.class);
    }

    @Override
    public WorkspaceToolModel build(Project project, BuildProperties properties) throws Exception {
        List<ExtraDataBuilder> dataBuilders = properties.getDataBuilders().stream()
                .map(clazz -> {
                    try {
                        return (ExtraDataBuilder) Class.forName(clazz).newInstance();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unable to find DataBuilder: " + clazz, e);
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException("Data builder '" + clazz + "' does not have a single public constructor.", e);
                    }
                })
                .collect(Collectors.toList());

        return new WorkspaceToolModelImpl(buildProject(project, null, dataBuilders));
    }

    public ProjectData buildProject(Project project, @Nullable ProjectData root, List<ExtraDataBuilder> dataBuilders) throws Exception {
        logger.debug("Building project: {}", project.getName());
        PluginData pluginData = buildPluginData(project);
        for (ExtraDataBuilder e : dataBuilders) {
            e.preBuild(project, pluginData);
        }

        ProjectData projectData = buildProjectData(project);
        projectData.pluginData = pluginData;

        ProjectData rootData = root == null ? projectData : root;
        for (ExtraDataBuilder e : dataBuilders) {
            e.build(project, projectData, rootData);
        }
        for (Project subProject : project.getSubprojects()) {
            projectData.subProjects.put(subProject.getName(), buildProject(subProject, rootData, dataBuilders));
        }
        return projectData;
    }

    private PluginData buildPluginData(Project project) throws Exception {
        PluginData pluginData = new PluginData();
        PluginContainer pluginContainer = project.getPlugins();
        Set<String> loadedPluginClasses = pluginContainer.stream()
                .map(Plugin::getClass)
                .map(Class::getName)
                .collect(Collectors.toSet());
        Map<String, String> classToName = PluginResolver.extractPlugins(pluginContainer);

        pluginData.pluginIds.addAll(classToName.entrySet().stream()
                .filter(e -> loadedPluginClasses.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
        );
        pluginData.pluginClasses.addAll(loadedPluginClasses);
        pluginData.classToName.putAll(classToName.entrySet().stream()
                .filter(e -> loadedPluginClasses.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        return pluginData;
    }

    private ProjectData buildProjectData(Project project) {
        ProjectData projectData = new ProjectData();
        projectData.name = project.getName();
        if (project.getParent() != null) {
            projectData.parent = buildProjectName(project.getParent());
        }
        projectData.projectDir = project.getProjectDir();
        projectData.version = String.valueOf(project.getVersion());
        projectData.group = String.valueOf(project.getGroup());
        projectData.archivesBaseName = String.valueOf(project.findProperty("archivesBaseName"));

        Map<String, ?> properties = project.getExtensions().getExtraProperties().getProperties();
        properties.forEach((k, v) -> {
            if (v instanceof CharSequence) {
                projectData.extraProperties.put(k, v.toString());
            }
        });

        buildConfigurationData(project, projectData);
        JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention != null) {
            for (SourceSet sourceSet : javaConvention.getSourceSets()) {
                SourceSetData data = new SourceSetData();
                projectData.sourceSets.put(sourceSet.getName(), data);
                data.name = sourceSet.getName();
                data.resources.addAll(getDirs(sourceSet.getResources()));
                data.getOrComputeSrc("java").addAll(getDirs(sourceSet.getJava()));

                //TODO, move to separate builder.
                Convention convention = (Convention) InvokerHelper.getProperty(sourceSet, "convention");
                ScalaSourceSet scalaSS = (ScalaSourceSet) convention.getPlugins().get("scala");
                if (scalaSS != null) {
                    data.getOrComputeSrc("scala").addAll(getDirs(scalaSS.getAllScala()));
                }

                //TODO, this is not ideal, we should more accurately build the runtime compile and runtime classpath.
                data.compileConfiguration = sourceSet.getCompileClasspathConfigurationName();
                data.runtimeConfiguration = sourceSet.getRuntimeClasspathConfigurationName();
                data.compileOnlyConfiguration = sourceSet.getCompileOnlyConfigurationName();
            }
        }

        return projectData;
    }

    private Set<File> getDirs(SourceDirectorySet dirSet) {
        return dirSet.getSrcDirs().stream().map(File::getAbsoluteFile).collect(Collectors.toSet());
    }

    private void buildConfigurationData(Project project, ProjectData projectData) {
        ConfigurationWalker walker = new ConfigurationWalker(project.getDependencies());
        Visitor visitor = new Visitor(projectData);
        walker.walk(project.getConfigurations(), visitor, FORCE, SOURCES, JAVADOC);
    }

    @Nullable
    private static String buildProjectName(@Nullable Project p) {
        if (p == null) return null;
        String name = p.getName();
        String pName = buildProjectName(p.getParent());
        // Use Colon as the separator character. This character is invalid for project names. See NameValidator in Gradle.
        return pName == null ? name : pName + ":" + name;
    }

    private static class Visitor implements ConfigurationVisitor {

        private final ProjectData projectData;
        @Nullable
        private ConfigurationData data;

        private Visitor(ProjectData projectData) {
            this.projectData = projectData;
        }

        @Override
        public void visitStart(Configuration configuration) {
            if (data != null) {
                throw new RuntimeException("Already visiting.");
            }
            data = new ConfigurationData(configuration.getName());
            projectData.configurations.put(configuration.getName(), data);
            data.extendsFrom.addAll(configuration.getExtendsFrom().stream().map(Configuration::getName).collect(Collectors.toSet()));
        }

        @Override
        public void visitModuleDependency(MavenNotation notation, File classes, @Nullable File sources, @Nullable File javadoc) {
            data.dependencies.add(new ConfigurationData.MavenDependency(
                    notation,
                    classes,
                    sources,
                    javadoc
            ));
        }

        @Override
        public void visitSourceSetDependency(SourceSet ss) {
            data.dependencies.add(new ConfigurationData.SourceSetDependency(ss.getName()));
        }

        @Override
        public void visitProjectDependency(Project project) {
            data.dependencies.add(new ConfigurationData.ProjectDependency(buildProjectName(project)));
        }

        @Override
        public void visitEnd() {
            data = null;
        }
    }

}
