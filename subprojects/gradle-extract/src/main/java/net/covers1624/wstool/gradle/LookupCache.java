package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by covers1624 on 9/9/23.
 */
// TODO replace with a typed map/blackboard?
public class LookupCache {

    /**
     * Used to lookup {@link ProjectData} instances from its originating {@link Project}
     * when parsing project dependencies.
     */
    public final Map<String, ProjectData> projects = new LinkedHashMap<>();
    /**
     * Used to lookup {@link SourceSetData} instances from its originating {@link SourceSetOutput}
     * when parsing source set dependencies.
     */
    public final Map<SourceSetOutput, SourceSetData> sourceSets = new LinkedHashMap<>();
}
