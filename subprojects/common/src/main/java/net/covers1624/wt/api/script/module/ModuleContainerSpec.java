package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by covers1624 on 23/05/19.
 */
public interface ModuleContainerSpec {

    default void caseSensitive(boolean caseSensitive) {
        setCaseSensitive(caseSensitive);
    }

    void setCaseSensitive(boolean caseSensitive);

    void include(String... includes);

    default void include(String include, Closure<ModuleSpec> closure) {
        include(include, new ClosureBackedConsumer<>(closure));
    }

    void include(String include, Consumer<ModuleSpec> consumer);

    void exclude(String... excludes);

    boolean getCaseSensitive();

    Set<String> getIncludes();

    Set<String> getExcludes();

    Map<String, ModuleSpec> getCustomModules();

    Predicate<Path> createMatcher();
}
