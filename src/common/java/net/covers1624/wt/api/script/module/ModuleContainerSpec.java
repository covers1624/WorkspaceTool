package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 23/05/19.
 */
public interface ModuleContainerSpec {

    default void group(String name, Closure<ModuleGroupSpec> closure) {
        group(name, new ClosureBackedConsumer<>(closure));
    }

    void group(String name, Consumer<ModuleGroupSpec> consumer);
}
