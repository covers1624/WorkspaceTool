package net.covers1624.wstool.gradle.api.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains data from the {@code ext} Gradle extension.
 * <p>
 * Created by covers1624 on 16/5/23.
 */
public class ProjectExtData extends Data {

    public final Map<String, String> properties = new LinkedHashMap<>();
}
