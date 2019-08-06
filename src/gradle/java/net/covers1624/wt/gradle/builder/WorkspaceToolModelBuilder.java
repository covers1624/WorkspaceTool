package net.covers1624.wt.gradle.builder;

import net.covers1624.gradlestuff.dependencies.ConfigurationVisitor;
import net.covers1624.gradlestuff.dependencies.ConfigurationWalker;
import net.covers1624.gradlestuff.dependencies.DependencyName;
import net.covers1624.wt.api.data.*;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.gradle.model.impl.WorkspaceToolModelImpl;
import net.covers1624.wt.event.VersionedClass;
import net.covers1624.wt.gradle.util.PluginResolver;
import net.covers1624.wt.util.MavenNotation;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.ScalaSourceSet;
import org.gradle.api.tasks.SourceSet;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (4)
@SuppressWarnings ("UnstableApiUsage")
public class WorkspaceToolModelBuilder extends AbstractModelBuilder<WorkspaceToolModel> {

    private static Logger logger = (Logger) LoggerFactory.getLogger("WorkspaceToolModelBuilder");

    public WorkspaceToolModelBuilder() {
        super(WorkspaceToolModel.class);
    }

    @Override
    public WorkspaceToolModel build(Project project, BuildProperties properties) throws Exception {
        List<ExtraDataBuilder> dataBuilders = properties.getDataBuilders().stream()//
                .map(clazz -> {//
                    try {
                        return (ExtraDataBuilder) Class.forName(clazz).newInstance();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unable to find DataBuilder: " + clazz, e);
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException("Data builder '" + clazz + "' does not have a single public constructor.", e);
                    }
                })//
                .collect(Collectors.toList());
        PluginData pluginData = buildPluginData(project);

        for (ExtraDataBuilder e : dataBuilders) {
            e.preGradleData(project, pluginData);
        }

        GradleData gradleData = buildGradleData(project);

        for (ExtraDataBuilder b : dataBuilders) {
            b.build(project, pluginData, gradleData);
        }

        return new WorkspaceToolModelImpl(pluginData, gradleData);
    }

    private PluginData buildPluginData(Project project) throws Exception {
        PluginData pluginData = new PluginData();
        PluginResolver resolver = new PluginResolver();
        PluginContainer pluginContainer = project.getPlugins();
        resolver.process(pluginContainer);
        Set<String> loadedPluginClasses = pluginContainer.parallelStream()//
                .map(Plugin::getClass)//
                .map(Class::getName)//
                .collect(Collectors.toSet());//
        Map<String, String> classToName = resolver.getClassNameMappings();

        pluginData.pluginIds.addAll(classToName.entrySet().parallelStream()//
                .filter(e -> loadedPluginClasses.contains(e.getKey()))//
                .map(Map.Entry::getValue)//
                .collect(Collectors.toSet())//
        );
        pluginData.pluginClasses.addAll(loadedPluginClasses);
        pluginData.classToName.putAll(classToName.entrySet().parallelStream()//
                .filter(e -> loadedPluginClasses.contains(e.getKey()))//
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))//
        );
        return pluginData;
    }

    private GradleData buildGradleData(Project project) {
        GradleData gradleData = new GradleData();
        gradleData.group = String.valueOf(project.getGroup());
        gradleData.archivesBaseName = String.valueOf(project.getProperties().get("archivesBaseName"));
        buildConfigurationData(project, gradleData);
        JavaPluginConvention javaConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaConvention != null) {
            for (SourceSet sourceSet : javaConvention.getSourceSets()) {
                SourceSetData data = new SourceSetData();
                gradleData.sourceSets.put(sourceSet.getName(), data);
                data.name = sourceSet.getName();
                data.resources.addAll(getDirs(sourceSet.getResources()));
                data.getOrComputeSrc("java").addAll(getDirs(sourceSet.getJava()));

                //TODO, move to separate builder.
                Convention convention = (Convention) InvokerHelper.getProperty(sourceSet, "convention");
                ScalaSourceSet scalaSS = (ScalaSourceSet) convention.getPlugins().get("scala");
                if (scalaSS != null) {
                    data.getOrComputeSrc("scala").addAll(getDirs(scalaSS.getAllScala()));
                }

                data.compileConfiguration = sourceSet.getCompileConfigurationName();
                data.runtimeConfiguration = sourceSet.getRuntimeOnlyConfigurationName();
                data.compileOnlyConfiguration = sourceSet.getCompileOnlyConfigurationName();
            }
        }

        return gradleData;
    }

    private Set<File> getDirs(SourceDirectorySet dirSet) {
        return dirSet.getSrcDirs().stream().map(File::getAbsoluteFile).collect(Collectors.toSet());
    }

    private void buildConfigurationData(Project project, GradleData gradleData) {
        ConfigurationWalker walker = new ConfigurationWalker(project.getDependencies());
        Visitor visitor = new Visitor(gradleData);
        walker.walk(project.getConfigurations(), visitor);
    }

    private static class Visitor implements ConfigurationVisitor {

        private final GradleData gradleData;
        private ConfigurationData data;

        private Visitor(GradleData gradleData) {
            this.gradleData = gradleData;
        }

        @Override
        public void startVisit(Configuration configuration) {
            if (data != null) {
                throw new RuntimeException("Already visiting.");
            }
            data = new ConfigurationData();
            gradleData.configurations.put(configuration.getName(), data);
            data.name = configuration.getName();
            data.transitive = configuration.isTransitive();
            data.extendsFrom = configuration.getExtendsFrom().stream().map(Configuration::getName).collect(Collectors.toSet());
        }

        @Override
        public void visitModuleDependency(DependencyName name, File classes, File sources, File javadoc) {
            ConfigurationData.MavenDependency dep = new ConfigurationData.MavenDependency();
            dep.mavenNotation = new MavenNotation(name.getGroup(), name.getModule(), name.getVersion(), name.getClassifier(), name.getExtension());
            dep.sources = sources;
            dep.classes = classes;
            dep.javadoc = javadoc;
            data.dependencies.add(dep);
        }

        @Override
        public void visitSourceSetDependency(SourceSet ss) {
            ConfigurationData.SourceSetDependency dep = new ConfigurationData.SourceSetDependency();
            data.dependencies.add(dep);
            dep.name = ss.getName();
        }

        @Override
        public void endVisit() {
            data = null;
        }
    }

}
