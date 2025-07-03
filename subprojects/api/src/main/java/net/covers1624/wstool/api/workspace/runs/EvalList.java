package net.covers1624.wstool.api.workspace.runs;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A wrapper around {@link List} to assist with
 * adding {@link String} values and {@link EvalValue} values.
 * <p>
 * Created by covers1624 on 5/28/25.
 */
public class EvalList {

    private final List<EvalValue> values = new ArrayList<>();

    public void add(String str) {
        addEval(new EvalValue.StringValue(str));
    }

    public void addEval(EvalValue value) {
        values.add(value);
    }

    public void addAll(List<String> entries) {
        entries.forEach(e -> values.add(new EvalValue.StringValue(e)));
    }

    public void addAllEval(List<EvalValue> entries) {
        values.addAll(entries);
    }

    public void addFirst(String str) {
        addFirst(List.of(str));
    }

    public void addFirstEval(EvalValue value) {
        addFirstEval(List.of(value));
    }

    public void addFirst(List<String> entries) {
        addFirstEval(FastStream.of(entries).map(EvalValue.StringValue::new).toList(FastStream.infer()));
    }

    public void addFirstEval(List<EvalValue> entries) {
        values.addAll(0, entries);
    }

    public List<EvalValue> toList() {
        return Collections.unmodifiableList(values);
    }

    public List<String> toList(Function<Dependency, @Nullable Path> depFunc) {
        return FastStream.of(values)
                .map(e -> e.eval(depFunc))
                .toList();
    }
}
