package net.covers1624.wt.api.impl.module;

import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.data.PluginData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.GradleBackedModule;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.gradle.GradleModelCacheImpl;
import net.covers1624.wt.util.GradleModuleModelHelper;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 25/05/19.
 */
public class ModuleImpl implements Module {

    private final String name;
    private final String group;
    private final Path path;
    private final Map<String, SourceSet> sourceSets;
    private final Map<String, Configuration> configurations;

    public ModuleImpl(String name, String group, Path path) {
        this.name = name;
        this.group = group;
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
    public String getGroup() {
        return group;
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

        private final PluginData pluginData;
        private final GradleData gradleData;

        public GradleModule(String name, String group, Path path, PluginData pluginData, GradleData gradleData) {
            super(name, group, path);
            this.pluginData = pluginData;
            this.gradleData = gradleData;
        }

        @Override
        public PluginData getPluginData() {
            return pluginData;
        }

        @Override
        public GradleData getGradleData() {
            return gradleData;
        }
    }

    public static Module makeGradleModule(String name, String group, Path path, GradleModelCacheImpl modelCache) {
        WorkspaceToolModel model = modelCache.getModel(path, Collections.emptySet());//TODO
        GradleModule module = new GradleModule(name, group, path, model.getPluginData(), model.getGradleData());
        GradleModuleModelHelper.populateModule(module, model);
        return module;
    }
}
