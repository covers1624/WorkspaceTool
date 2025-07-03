package net.covers1624.wstool.api.workspace.runs;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Some value, when evaluated, converts to a {@link String}.
 * <p>
 * This basically only exists so the in-use workspace module can define how/where {@link Dependency Dependencies}
 * are located on disk, be it a Module output, or Maven dependency.
 * <p>
 * Created by covers1624 on 5/28/25.
 */
// TODO we may be able to un-seal this if we add a `Iterable<Dependency> getDependencies()` function.
public sealed interface EvalValue permits EvalValue.StringValue, EvalValue.ClasspathValue {

    String eval(Function<Dependency, @Nullable Path> depFunc);

    record StringValue(String str) implements EvalValue {

        @Override
        public String eval(Function<Dependency, @Nullable Path> depFunc) {
            return str;
        }
    }

    record ClasspathValue(Set<Dependency> dependencies) implements EvalValue {

        @Override
        public String eval(Function<Dependency, @Nullable Path> depFunc) {
            return FastStream.of(dependencies)
                    .map(depFunc)
                    .filter(Objects::nonNull)
                    .map(e -> e.toAbsolutePath().toString())
                    .join(File.pathSeparator);
        }
    }
}
