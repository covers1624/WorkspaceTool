package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.LookupCache;
import net.covers1624.wstool.gradle.ProjectTransformer;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.databuild.SourceSetDataBuilder;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.publish.maven.MavenDependency;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 1/12/25.
 */
public class NeoForgeProjectTransformer implements ProjectTransformer {

    @Override
    public void transformProject(Project project, ProjectData projectData) {
        PluginData pluginData = projectData.pluginData();
        NeoForgeGradleVersion version = pluginData.getData(NeoForgeGradleVersion.class);
        if (version == null) return;

        if (version.variant == NeoForgeGradleVersion.Variant.NEO_GRADLE) {
            filterExtendsFrom(project, e -> e.equals("minecraft"));
            filterExtendsFrom(project, e -> e.startsWith("ng_dummy_ng_"));
            filterClasspaths(project, e -> "net.neoforged".equals(e.getGroup()) && "neoforge".equals(e.getName()));
        } else if (version.variant == NeoForgeGradleVersion.Variant.MOD_DEV_GRADLE) {
            filterExtendsFrom(project, e -> e.startsWith("modDev"));
        }
    }

    private static void filterExtendsFrom(Project project, Predicate<String> toRemove) {
        for (Configuration configuration : project.getConfigurations()) {
            if (ColUtils.anyMatch(configuration.getExtendsFrom(), e -> toRemove.test(e.getName()))) {
                List<Configuration> newExtendsFrom = FastStream.of(configuration.getExtendsFrom())
                        .filterNot(e -> toRemove.test(e.getName()))
                        .toList();
                configuration.setExtendsFrom(newExtendsFrom);
            }
        }
    }

    private static void filterClasspaths(Project project, Predicate<Dependency> predicate) {
        ConfigurationContainer configurations = project.getConfigurations();
        SourceSetContainer sourceSets = SourceSetDataBuilder.getSourceSetContainer(project);
        if (sourceSets == null) return;

        for (SourceSet sourceSet : sourceSets) {
            filterClasspath(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), predicate);
            filterClasspath(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), predicate);
        }
    }

    private static void filterClasspath(Configuration configuration, Predicate<Dependency> predicate) {
        for (Configuration config : configuration.getHierarchy()) {
            config.getDependencies().removeIf(predicate);
        }
    }
}
