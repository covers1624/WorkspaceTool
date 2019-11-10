package net.covers1624.wt.api.impl.dependency;

import com.google.common.collect.Iterables;
import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.dependency.LibraryDependency;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.util.MavenNotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 10/7/19.
 */
public class DependencyLibraryImpl implements DependencyLibrary {

    private Map<MavenNotation, LibraryDependency> dependencies = Collections.synchronizedMap(new HashMap<>());
    private Map<String, LibraryDependency> scalaDependencies = Collections.synchronizedMap(new HashMap<>());

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
        config.walkHierarchy(c -> c.setDependencies(c.getDependencies().stream()//
                .map(e -> {
                    if (e instanceof MavenDependency) {
                        return resolve((MavenDependency) e);
                    }
                    if (e instanceof ScalaSdkDependency) {
                        return resolve((ScalaSdkDependency) e);
                    }
                    return e;
                })//
                .collect(Collectors.toSet())//
        ));
    }

    @Override
    public LibraryDependency resolve(MavenDependency mavenDep) {
        return dependencies.computeIfAbsent(mavenDep.getNotation(), e ->//
                new LibraryDependencyImpl()//
                        .setDependency(mavenDep)//
                        .setLibraryName(e.toString())//
        );
    }

    @Override
    public LibraryDependency resolve(ScalaSdkDependency scalaSdk) {
        return scalaDependencies.computeIfAbsent(scalaSdk.getVersion(), e ->//
                new LibraryDependencyImpl()//
                        .setDependency(scalaSdk)//
                        .setLibraryName("scala-sdk-" + scalaSdk.getVersion())//
        );
    }
}
