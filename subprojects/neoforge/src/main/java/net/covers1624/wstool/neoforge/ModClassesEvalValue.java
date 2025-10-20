package net.covers1624.wstool.neoforge;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.runs.EvalValue;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by covers1624 on 7/3/25.
 */
public record ModClassesEvalValue(List<ModClass> entries) implements EvalValue {

    @Override
    public String eval(Function<Dependency, @Nullable Path> depFunc) {
        return FastStream.of(entries)
                .flatMap(e -> FastStream.of(e.dependencies)
                        .map(d -> e.name + "%%" +depFunc.apply(d).toAbsolutePath())
                )
                .join(":");
    }

    @Override
    public Iterable<Dependency> collectDependencies() {
        return FastStream.of(entries).flatMap(e -> e.dependencies);
    }

    public record ModClass(String name, List<Dependency> dependencies) {

        public ModClass(String name, Dependency classesAndResources) {
            this(name, List.of(classesAndResources, classesAndResources));
        }
    }
}
