package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.script.module.DependenciesSpec;
import net.covers1624.wt.api.script.module.SourceSetSpec;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 27/05/19.
 */
public class SourceSetSpecImpl implements SourceSetSpec {

    private final Set<Path> sources = new HashSet<>();
    private final Set<Path> resources = new HashSet<>();
    private final DependenciesSpecImpl dependencies = new DependenciesSpecImpl();

    @Override
    public void src(Path path) {
        sources.add(path);
    }

    @Override
    public void resources(Path path) {
        resources.add(path);
    }

    @Override
    public void dependencies(Consumer<DependenciesSpec> consumer) {
        consumer.accept(dependencies);
    }
}
