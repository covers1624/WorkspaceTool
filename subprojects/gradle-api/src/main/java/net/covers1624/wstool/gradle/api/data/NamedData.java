package net.covers1624.wstool.gradle.api.data;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by covers1624 on 10/9/23.
 */
public abstract class NamedData<T extends Data> extends Data {

    private final Map<String, T> namedData = new LinkedHashMap<>();

    public T put(String name, T data) {
        namedData.put(name, data);
        return data;
    }

    public @Nullable T get(String name) {
        return namedData.get(name);
    }

    public T computeIfAbsent(String name, Function<String, T> func) {
        return namedData.computeIfAbsent(name, func);
    }

    public Map<String, T> asMap() {
        return namedData;
    }
}
