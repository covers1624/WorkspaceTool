package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 27/05/19.
 */
public interface SourceSetSpec {

    void src(Path path);

    void resources(Path path);

    default void dependencies(Closure<DependenciesSpec> closure) {
        dependencies(new ClosureBackedConsumer<>(closure));
    }

    void dependencies(Consumer<DependenciesSpec> consumer);
}
