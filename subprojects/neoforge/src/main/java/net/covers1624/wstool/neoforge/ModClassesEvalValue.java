package net.covers1624.wstool.neoforge;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.DependencyResolver;
import net.covers1624.wstool.api.workspace.runs.EvalValue;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 7/3/25.
 */
public record ModClassesEvalValue(List<ModClass> entries) implements EvalValue {

    @Override
    public String eval(DependencyResolver depResolver) {
        return FastStream.of(entries)
                .flatMap(e -> FastStream.of(e.dependencies)
                        .map(d -> e.name + "%%" + requireNonNull(depResolver.resolvePath(d)).toAbsolutePath())
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
