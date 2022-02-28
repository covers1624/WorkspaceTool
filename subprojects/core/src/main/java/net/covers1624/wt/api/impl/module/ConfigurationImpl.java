/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.module;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.module.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by covers1624 on 30/6/19.
 */
// TODO, Dependency transitivity needs to be fixed.
//  At the moment we ignore dependency transitivity in some cases. We will likely need to
//  model dependency transitives in Gradle extraction data. We can then resolve transitives
//  this side.
public class ConfigurationImpl implements Configuration {

    private final String name;
    private final Set<Dependency> dependencies = new LinkedHashSet<>();
    private final Set<Configuration> extendsFrom = new LinkedHashSet<>();

    public ConfigurationImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Configuration> getExtendsFrom() {
        return extendsFrom;
    }

    @Override
    public void addExtendsFrom(Configuration configuration) {
        extendsFrom.add(configuration);
    }

    @Override
    public void setExtendsFrom(Set<Configuration> extendsFrom) {
        this.extendsFrom.clear();
        this.extendsFrom.addAll(extendsFrom);
    }

    @Override
    public Set<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    @Override
    public void setDependencies(Set<Dependency> dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(dependencies);
    }

    @Override
    public void addDependencies(Set<Dependency> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof Configuration)) {
            return false;
        }
        return ((Configuration) obj).getName().equals(getName());
    }
}
