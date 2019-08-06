package net.covers1624.wt.api.script.module;

import groovy.lang.Closure;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 27/05/19.
 */
public interface ModuleSpec {

    void setDir(Path path);

    Path getDir();

    default void dir(Path path) {
        setDir(path);
    }

    default void sourceSet(String name, Closure<SourceSetSpec> closure) {
        sourceSet(name, new ClosureBackedConsumer<>(closure));
    }

    void sourceSet(String name, Consumer<SourceSetSpec> consumer);

}
