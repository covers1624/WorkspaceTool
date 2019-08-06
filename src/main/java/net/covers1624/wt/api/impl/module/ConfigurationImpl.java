package net.covers1624.wt.api.impl.module;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.module.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by covers1624 on 30/6/19.
 */
public class ConfigurationImpl implements Configuration {

    private final String name;
    private final boolean transitive;
    private final List<Dependency> dependencies = new ArrayList<>();
    private Set<Configuration> extendsFrom = new HashSet<>();

    public ConfigurationImpl(String name, boolean transitive) {
        this.name = name;
        this.transitive = transitive;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTransitive() {
        return transitive;
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
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    @Override
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies.clear();
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
