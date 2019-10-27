package net.covers1624.wt.api.data;

import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.event.VersionedClass;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Implemented by {@link ProjectData} and {@link PluginData} to hold data built by ExtraModelBuilders.
 *
 * @see GradleManager
 *
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (1)
public abstract class ExtraDataExtensible implements Serializable {

    public Map<Class<? extends ExtraData>, ExtraData> extraData = new HashMap<>();

    @SuppressWarnings ("unchecked")
    public <T extends ExtraData> T getData(Class<T> type) {
        return (T) extraData.get(type);
    }

}
