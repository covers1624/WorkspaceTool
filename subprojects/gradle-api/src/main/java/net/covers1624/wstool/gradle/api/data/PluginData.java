package net.covers1624.wstool.gradle.api.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 15/5/23.
 */
public class PluginData extends Data implements Serializable {

    /**
     * A map of Gradle plugin id -> class.
     * <p>
     * In the event that an id cannot be resolved for a given class,
     * the class name will be used as the key instead.
     */
    public final Map<String, String> plugins = new HashMap<>();
}
