package net.covers1624.wt.api.impl.dependency;

import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.dependency.LibraryDependency;
import net.covers1624.wt.api.dependency.MavenDependency;
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

    @Override
    public Map<MavenNotation, LibraryDependency> getDependencies() {
        return Collections.unmodifiableMap(dependencies);
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
                    return e;
                })//
                .collect(Collectors.toSet())//
        ));
    }

    @Override
    public LibraryDependency resolve(MavenDependency mavenDep) {
        return dependencies.computeIfAbsent(mavenDep.getNotation(), e ->//
                new LibraryDependencyImpl()//
                        .setMavenDependency(mavenDep)//
                        .setLibraryName(e.toString())//
        );
    }
}
