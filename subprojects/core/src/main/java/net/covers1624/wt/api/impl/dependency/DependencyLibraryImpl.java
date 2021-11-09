/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.dependency;

import com.google.common.collect.Iterables;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.dependency.LibraryDependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 10/7/19.
 */
public class DependencyLibraryImpl implements DependencyLibrary {

    private final Map<MavenNotation, LibraryDependency> dependencies = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, LibraryDependency> scalaDependencies = Collections.synchronizedMap(new HashMap<>());

    @Override
    public Iterable<LibraryDependency> getDependencies() {
        return Iterables.unmodifiableIterable(Iterables.concat(dependencies.values(), scalaDependencies.values()));
    }

    @Override
    public void consume(Module module) {
        module.getSourceSets().values().forEach(ss -> {
            consume(ss.getCompileConfiguration());
            consume(ss.getRuntimeConfiguration());
            consume(ss.getCompileOnlyConfiguration());
        });
    }

    private void consume(Configuration config) {
        if (config == null) {
            return;
        }
        config.streamAll().forEach(c -> c.setDependencies(c.getDependencies().stream()
                .map(e -> {
                    if (e instanceof MavenDependency dep) {
                        return resolve(dep);
                    }
                    if (e instanceof ScalaSdkDependency dep) {
                        return resolve(dep);
                    }
                    return e;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new))
        ));
    }

    @Override
    public LibraryDependency resolve(MavenDependency mavenDep) {
        return dependencies.computeIfAbsent(mavenDep.getNotation(), e ->
                new LibraryDependencyImpl()
                        .setDependency(mavenDep)
                        .setLibraryName(e.toString())
        );
    }

    @Override
    public LibraryDependency resolve(ScalaSdkDependency scalaSdk) {
        return scalaDependencies.computeIfAbsent(scalaSdk.getVersion(), e ->
                new LibraryDependencyImpl()
                        .setDependency(scalaSdk)
                        .setLibraryName("scala-sdk-" + scalaSdk.getVersion())
        );
    }
}
