package net.covers1624.wstool.api.workspace.runs;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Dependency;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by covers1624 on 5/28/25.
 */
public class EvalMap {

    private final LinkedHashMap<String, EvalValue> values = new LinkedHashMap<>();

    public void putFirst(String key, String value) {
        putEvalFirst(key, new EvalValue.StringValue(value));
    }

    public void put(String key, String value) {
        putEval(key, new EvalValue.StringValue(value));
    }

    public void putEvalFirst(String key, EvalValue value) {
        values.putFirst(key, value);
    }

    public void putEval(String key, EvalValue value) {
        values.put(key, value);
    }

    public void putAll(Map<String, String> values) {
        values.forEach((k, v) -> this.values.put(k, new EvalValue.StringValue(v)));
    }

    public void putAllEval(Map<String, EvalValue> values) {
        this.values.putAll(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, EvalValue> toMap() {
        return Collections.unmodifiableMap(values);
    }

    public Map<String, String> toMap(Function<Dependency, @Nullable Path> depFunc) {
        return FastStream.of(values.entrySet())
                .toLinkedHashMap(Map.Entry::getKey, e -> e.getValue().eval(depFunc));
    }
}
