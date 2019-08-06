package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 23/05/19.
 */
public interface ModuleGroupSpec {

    void caseSensitive(boolean caseSensitive);

    void include(String... includes);

    void exclude(String... excludes);

    default void module(String name, Closure<ModuleSpec> closure) {
        module(name, new ClosureBackedConsumer<>(closure));
    }

    void module(String name, Consumer<ModuleSpec> consumer);
}
