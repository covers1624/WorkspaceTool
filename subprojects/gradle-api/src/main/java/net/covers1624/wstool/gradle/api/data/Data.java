package net.covers1624.wstool.gradle.api.data;

import net.covers1624.quack.util.SneakyUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 15/5/23.
 */
public abstract class Data implements Serializable {

    /**
     * Map of data objects build into/from the current data object.
     */
    private final Map<Class<? extends Data>, Data> data = new HashMap<>();

    /**
     * Add a data object of a specific type.
     *
     * @param clazz The type.
     * @param data  The data.
     */
    public <T extends Data> T putData(Class<T> clazz, T data) {
        this.data.put(clazz, data);
        return data;
    }

    /**
     * Get a data object of a specific type.
     *
     * @param clazz The type of data object to retrieve.
     * @return The data object.
     */
    @Nullable
    public <T extends Data> T getData(Class<T> clazz) {
        return SneakyUtils.unsafeCast(data.get(clazz));
    }
}
