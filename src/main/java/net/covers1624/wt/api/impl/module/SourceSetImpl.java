package net.covers1624.wt.api.impl.module;

import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.SourceSet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 30/6/19.
 */
public class SourceSetImpl implements SourceSet {

    private final String name;
    private final List<Path> resources = new ArrayList<>();
    private final Map<String, List<Path>> sourceMap = new HashMap<>();
    private Configuration compileConfiguration;
    private Configuration runtimeConfiguration;
    private Configuration compileOnlyConfiguration;

    public SourceSetImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Path> getResources() {
        return resources;
    }

    @Override
    public void addResource(Path path) {
        resources.add(path);
    }

    @Override
    public void setResources(List<Path> paths) {
        resources.clear();
        resources.addAll(paths);
    }

    @Override
    public Map<String, List<Path>> getSourceMap() {
        return sourceMap;
    }

    @Override
    public void addSource(String name, List<Path> paths) {
        this.sourceMap.put(name, new ArrayList<>(paths));
    }

    @Override
    public void setSourceMap(Map<String, List<Path>> sourceMap) {
        this.sourceMap.clear();
        this.sourceMap.putAll(sourceMap);
        this.sourceMap.replaceAll(((k, v) -> new ArrayList<>(v)));
    }

    @Override
    public Configuration getCompileConfiguration() {
        return compileConfiguration;
    }

    @Override
    public void setCompileConfiguration(Configuration configuration) {
        compileConfiguration = configuration;
    }

    @Override
    public Configuration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    @Override
    public void setRuntimeConfiguration(Configuration configuration) {
        runtimeConfiguration = configuration;
    }

    @Override
    public Configuration getCompileOnlyConfiguration() {
        return compileOnlyConfiguration;
    }

    @Override
    public void setCompileOnlyConfiguration(Configuration configuration) {
        compileOnlyConfiguration = configuration;
    }
}
