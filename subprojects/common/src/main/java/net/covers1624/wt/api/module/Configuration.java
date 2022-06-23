/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.module;

import com.google.common.collect.Streams;
import net.covers1624.wt.api.dependency.Dependency;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a Gradle Configuration.
 * <p>
 * Created by covers1624 on 30/6/19.
 */
public interface Configuration {

    /**
     * @return The name of the Configuration.
     */
    String getName();

    /**
     * Any Configurations that this Configuration extends from.
     *
     * @return The Extended Configurations
     */
    Set<Configuration> getExtendsFrom();

    /**
     * Add a Configuration to the Extended-From set.
     *
     * @param configuration The Configuration.
     */
    void addExtendsFrom(Configuration configuration);

    /**
     * Replaces all Configurations in the Extended-From Set with the provided Set.
     *
     * @param extendsFrom The set.
     */
    void setExtendsFrom(Set<Configuration> extendsFrom);

    /**
     * Stream this Configuration and all configurations it extends from.
     */
    default Stream<Configuration> streamAll() {
        return Streams.concat(Stream.of(this), getExtendsFrom().stream().flatMap(Configuration::streamAll))
                .distinct();
    }

    /**
     * Gets any Dependencies provided by this Configuration.
     *
     * @return The Dependencies.
     */
    Set<Dependency> getDependencies();

    /**
     * Adds a {@link Dependency} to this Configuration.
     *
     * @param dependency The {@link Dependency}.
     */
    void addDependency(Dependency dependency);

    /**
     * Replaces all Dependencies provided by this Configuration with the provided list.
     *
     * @param dependencies The Dependencies.
     */
    void setDependencies(Set<Dependency> dependencies);

    void addDependencies(Set<Dependency> dependencies);

    /**
     * Gets all transitive Dependencies provided by this Configuration.
     *
     * @return The Dependencies.s
     */
    default Set<Dependency> getAllDependencies() {
        return streamAll()
                .flatMap(e -> e.getDependencies().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
