package net.covers1624.wt.api.impl.module;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.gradle.data.ProjectData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.event.ProcessProjectDataEvent;
import net.covers1624.wt.util.ProjectDataHelper;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 25/05/19.
 */
public class ModuleImpl implements Module {

    private final String name;
    private final Path path;
    private final Map<String, SourceSet> sourceSets;
    private final Map<String, Configuration> configurations;
    private Path compileOutput;
    private boolean modulePerSourceSet;

    public ModuleImpl(String name, Path path) {
        this.name = name;
        this.path = path;
        this.sourceSets = new HashMap<>();
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
    public Path getCompileOutput() {
        return compileOutput;
    }

    @Override
    public void setCompileOutput(Path compileOutput) {
        this.compileOutput = compileOutput;
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

    @Override
    public boolean getModulePerSourceSet() {
        return modulePerSourceSet;
    }

    @Override
    public void setModulePerSourceSet(boolean value) {
        modulePerSourceSet = value;
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

    public static Module makeGradleModule(String name, Path path, WorkspaceToolContext context) {
        WorkspaceToolModel model = context.modelCache.getModel(path, Collections.emptySet());//TODO
        ProcessProjectDataEvent.REGISTRY.fireEvent(new ProcessProjectDataEvent(context, model.getProjectData()));
        GradleModule module = new GradleModule(name, path, model.getProjectData());
        ProjectDataHelper.buildModule(module, model);
        return module;
    }
}
