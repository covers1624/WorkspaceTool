/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.module;

import com.google.common.collect.ImmutableList;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.event.ProcessProjectDataEvent;
import net.covers1624.wt.util.ProjectDataHelper;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.covers1624.quack.collection.ColUtils.iterable;

/**
 * Created by covers1624 on 25/05/19.
 */
public class ModuleImpl implements Module {

    private final String name;
    private final Path path;
    private final Map<String, SourceSet> sourceSets;
    private final List<Path> excludes;
    private final Map<String, Configuration> configurations;

    public ModuleImpl(String name, Path path) {
        this.name = name;
        this.path = path;
        this.sourceSets = new HashMap<>();
        this.excludes = new ArrayList<>();
        this.configurations = new HashMap<>();
        if (!path.getFileSystem().provider().getScheme().equals("file")) {
            throw new RuntimeException("Path is not on the default filesystem.");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public Map<String, SourceSet> getSourceSets() {
        return sourceSets;
    }

    @Override
    public void addSourceSet(String name, SourceSet sourceSet) {
        sourceSets.put(name, sourceSet);
    }

    @Override
    public void setSourceSets(Map<String, SourceSet> sourceSets) {
        this.sourceSets.clear();
        this.sourceSets.putAll(sourceSets);
    }

    @Override
    public List<Path> getExcludes() {
        return excludes;
    }

    @Override
    public void addExclude(Path exclude) {
        excludes.add(exclude);
    }

    @Override
    public void setExcludes(List<Path> excludes) {
        this.excludes.clear();
        this.excludes.addAll(excludes);
    }

    @Override
    public Map<String, Configuration> getConfigurations() {
        return configurations;
    }

    @Override
    public void addConfiguration(String name, Configuration configuration) {
        configurations.put(name, configuration);
    }

    @Override
    public void setConfigurations(Map<String, Configuration> configurations) {
        this.configurations.clear();
        this.configurations.putAll(configurations);
    }

    public static class GradleModule extends ModuleImpl implements GradleBackedModule {

        private final ProjectData projectData;

        public GradleModule(String name, Path path, ProjectData projectData) {
            super(name, path);
            this.projectData = projectData;
        }

        @Override
        public ProjectData getProjectData() {
            return projectData;
        }
    }

    public static List<Module> makeGradleModules(String groupPrefix, ProjectData project, WorkspaceToolContext ctx) {
        Map<String, Module> modules = new HashMap<>();
        for (ProjectData p : iterable(project.streamAllProjects())) {
            ProcessProjectDataEvent.REGISTRY.fireEvent(new ProcessProjectDataEvent(ctx, p));
            modules.put(p.getProjectCoords(), new GradleModule(buildModuleName(groupPrefix, p), p.projectDir.toPath(), p));
        }
        return makeGradleModules(project, ctx, modules);
    }

    private static String buildModuleName(String groupPrefix, ProjectData data) {
        String name = data.getProjectCoords().replace(':', '/');
        if (!groupPrefix.isEmpty()) {
            return StringUtils.appendIfMissing(groupPrefix, "/") + name;
        }
        return name;
    }

    private static List<Module> makeGradleModules(ProjectData project, WorkspaceToolContext ctx, Map<String, Module> modules) {
        Module module = modules.get(project.getProjectCoords());
        ProjectDataHelper.buildModule(module, project, modules);
        module.addExclude(module.getPath().resolve("build"));
        module.addExclude(module.getPath().resolve(".gradle"));
        ImmutableList.Builder<Module> moduleList = ImmutableList.builder();
        moduleList.add(module);
        for (ProjectData subProject : project.subProjects.values()) {
            moduleList.addAll(makeGradleModules(subProject, ctx, modules));
        }
        return moduleList.build();
    }
}
