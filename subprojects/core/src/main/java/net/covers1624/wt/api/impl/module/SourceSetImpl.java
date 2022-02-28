/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
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
    public void setSource(String name, List<Path> paths) {
        List<Path> ssPaths = sourceMap.computeIfAbsent(name, e -> new ArrayList<>());
        ssPaths.clear();
        ssPaths.addAll(paths);
    }

    @Override
    public void addSource(String name, List<Path> paths) {
        List<Path> ssPaths = sourceMap.computeIfAbsent(name, e -> new ArrayList<>());
        ssPaths.addAll(paths);
    }

    @Override
    public void addSource(String name, Path path) {
        List<Path> ssPaths = sourceMap.computeIfAbsent(name, e -> new ArrayList<>());
        ssPaths.add(path);
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
