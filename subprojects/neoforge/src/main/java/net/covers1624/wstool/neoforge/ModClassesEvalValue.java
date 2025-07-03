package net.covers1624.wstool.neoforge;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.runs.EvalValue;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by covers1624 on 7/3/25.
 */
public record ModClassesEvalValue(Map<String, ModClass> entries) implements EvalValue {

    @Override
    public String eval(Function<Dependency, @Nullable Path> depFunc) {
        return FastStream.of(entries.entrySet())
                .flatMap(e -> FastStream.of(
                        e.getKey() + "%%" + depFunc.apply(e.getValue().classes).toAbsolutePath(),
                        e.getKey() + "%%" + depFunc.apply(e.getValue().resources).toAbsolutePath()
                ))
                .join(":");
    }

    @Override
    public Iterable<Dependency> collectDependencies() {
        return FastStream.of(entries.values()).flatMap(e -> FastStream.of(e.classes, e.resources));
    }

    public record ModClass(Dependency classes, Dependency resources) {

        public ModClass(Dependency classesAndResources) {
            this(classesAndResources, classesAndResources);
        }

    }
}
