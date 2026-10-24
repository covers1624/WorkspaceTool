package net.covers1624.wstool.api.workspace.runs;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.DependencyResolver;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Some value, when evaluated, converts to a {@link String}.
 * <p>
 * This basically only exists so the in-use workspace module can define how/where {@link Dependency Dependencies}
 * are located on disk, be it a Module output, or Maven dependency.
 * <p>
 * Created by covers1624 on 5/28/25.
 */
public interface EvalValue {

    String eval(DependencyResolver depResolver);

    Iterable<Dependency> collectDependencies();

    record StringValue(String str) implements EvalValue {

        @Override
        public String eval(DependencyResolver depResolver) {
            return str;
        }

        @Override
        public Iterable<Dependency> collectDependencies() {
            return List.of();
        }
    }

    record ClasspathValue(Set<Dependency> dependencies) implements EvalValue {

        @Override
        public String eval(DependencyResolver depResolver) {
            return FastStream.of(dependencies)
                    .map(depResolver::resolvePath)
                    .filterNonNull()
                    .map(e -> e.toAbsolutePath().toString())
                    .join(File.pathSeparator);
        }

        @Override
        public Iterable<Dependency> collectDependencies() {
            return dependencies;
        }
    }
}
