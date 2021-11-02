/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.gradle.data.ConfigurationData;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ConfigurationImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static net.covers1624.wt.util.Utils.toPaths;

/**
 * Created by covers1624 on 8/7/19.
 */
public class ProjectDataHelper {

    public static void buildModule(Module module, ProjectData projectData, Map<String, Module> modules) {
        Map<String, Configuration> configurations = buildConfigurations(module, projectData, modules);
        Map<String, SourceSet> sourceSets = buildSourceSets(projectData, configurations);
        module.setSourceSets(sourceSets);
        module.setConfigurations(configurations);
    }

    public static Map<String, Configuration> buildConfigurations(Module module, ProjectData projectData, Map<String, Module> modules) {
        Map<String, Configuration> configurations = projectData.configurations.values().stream()
                .map(e -> convertConfiguration(e, module, modules))
                .collect(Collectors.toMap(Configuration::getName, identity()));
        for (ConfigurationData config : projectData.configurations.values()) {
            Set<Configuration> extendsFrom = config.extendsFrom.stream()
                    .map(configurations::get)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            configurations.get(config.name).setExtendsFrom(extendsFrom);
        }
        return configurations;
    }

    public static Configuration convertConfiguration(ConfigurationData config, Module module, Map<String, Module> modules) {
        Set<Dependency> dependencies = config.dependencies.stream()
                .map(d -> {
                    if (d instanceof ConfigurationData.MavenDependency) {
                        return new MavenDependencyImpl((ConfigurationData.MavenDependency) d);
                    } else if (d instanceof ConfigurationData.SourceSetDependency) {
                        return new SourceSetDependencyImpl()
                                .setSourceSet(((ConfigurationData.SourceSetDependency) d).name)
                                .setModule(module);
                    } else if (d instanceof ConfigurationData.ProjectDependency) {
                        ConfigurationData.ProjectDependency dep = (ConfigurationData.ProjectDependency) d;
                        return new SourceSetDependencyImpl()
                                .setSourceSet("main")
                                .setModule(requireNonNull(modules.get(dep.project), "Module missing: " + dep.project));
                    } else {
                        throw new RuntimeException("Unknown Dependency type from gradle-land: " + d.getClass());
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Configuration ret = new ConfigurationImpl(config.name);
        ret.setDependencies(dependencies);
        return ret;
    }

    public static Map<String, SourceSet> buildSourceSets(ProjectData projectData, Map<String, Configuration> configurations) {
        return projectData.sourceSets.values().stream()
                .map(sourceSet -> {
                    Map<String, List<Path>> sourceMap = new HashMap<>();
                    for (Map.Entry<String, List<File>> e : sourceSet.sourceMap.entrySet()) {
                        sourceMap.put(e.getKey(), toPaths(e.getValue()));
                    }
                    SourceSet ret = new SourceSetImpl(sourceSet.name);
                    ret.setResources(toPaths(sourceSet.resources));
                    ret.setSourceMap(sourceMap);
                    ret.setCompileConfiguration(configurations.get(sourceSet.compileConfiguration));
                    ret.setRuntimeConfiguration(configurations.get(sourceSet.runtimeConfiguration));
                    ret.setCompileOnlyConfiguration(configurations.get(sourceSet.compileOnlyConfiguration));
                    return ret;
                })
                .collect(Collectors.toMap(SourceSet::getName, identity()));
    }
}
