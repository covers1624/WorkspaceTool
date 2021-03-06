package net.covers1624.wt.util;

import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.data.SourceSetData;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ConfigurationImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.covers1624.wt.util.Utils.toPaths;

/**
 * Created by covers1624 on 8/7/19.
 */
public class ProjectDataHelper {

    public static void buildModule(Module module, WorkspaceToolModel model) {
        ProjectData projectData = model.getProjectData();
        Map<String, Configuration> configurations = buildConfigurations(module, projectData);
        Map<String, SourceSet> sourceSets = buildSourceSets(projectData, configurations);
        module.setSourceSets(sourceSets);
        module.setConfigurations(configurations);
    }

    public static Map<String, Configuration> buildConfigurations(Module module, ProjectData projectData) {
        Map<String, Configuration> configurations = projectData.configurations.entrySet().parallelStream()//
                .map(e -> convertConfiguration(e.getValue(), module))//
                .collect(Collectors.toMap(Configuration::getName, e -> e));
        projectData.configurations.values().parallelStream().forEach(e -> //
                configurations.get(e.name).setExtendsFrom(//
                        e.extendsFrom.parallelStream().map(configurations::get).collect(Collectors.toSet())//
                )//
        );
        return configurations;
    }

    public static Configuration convertConfiguration(ConfigurationData config, Module module) {
        Set<Dependency> dependencies = config.dependencies.parallelStream()//
                .map(d -> {//
                    if (d instanceof ConfigurationData.MavenDependency) {
                        return new MavenDependencyImpl((ConfigurationData.MavenDependency) d);
                    } else if (d instanceof ConfigurationData.SourceSetDependency) {
                        return new SourceSetDependencyImpl()//
                                .setSourceSet(((ConfigurationData.SourceSetDependency) d).name)//
                                .setModule(module);
                    } else {
                        throw new RuntimeException("Unknown Dependency type from gradle-land: " + d.getClass());
                    }
                })//
                .collect(Collectors.toSet());
        Configuration ret = new ConfigurationImpl(config.name, config.transitive);
        ret.setDependencies(dependencies);
        return ret;
    }

    public static Map<String, SourceSet> buildSourceSets(ProjectData projectData, Map<String, Configuration> configurations) {
        return projectData.sourceSets.entrySet().parallelStream()//
                .map(e -> {
                    SourceSetData sourceSet = e.getValue();
                    Map<String, List<Path>> sourceMap = sourceSet.sourceMap.entrySet().parallelStream()//
                            .map(s -> Pair.of(s.getKey(), toPaths(s.getValue())))//
                            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
                    SourceSet ret = new SourceSetImpl(sourceSet.name);
                    ret.setResources(toPaths(sourceSet.resources));
                    ret.setSourceMap(sourceMap);
                    ret.setCompileConfiguration(configurations.get(sourceSet.compileConfiguration));
                    ret.setRuntimeConfiguration(configurations.get(sourceSet.runtimeConfiguration));
                    ret.setCompileOnlyConfiguration(configurations.get(sourceSet.compileOnlyConfiguration));
                    return ret;
                })//
                .collect(Collectors.toMap(SourceSet::getName, e -> e));
    }
}
