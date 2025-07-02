package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetOutput;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
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
     * Plugin -> String List, of additional configurations to extract. These may be consumed wstool side
     * by a framework or workspace.
     */
    // TODO, this is really messy, we should find a better way to expose this contract from Plugin builder ->
    //       configuration extractor. Perhaps if this becomes a typed map/blackboard, we can expose a key of requests?
    public final Map<Project, List<String>> additionalConfigurations = new LinkedHashMap<>();

    /**
     * Used to lookup {@link SourceSetData} instances from its originating {@link SourceSetOutput}
     * when parsing source set dependencies.
     */
    public final Map<SourceSetOutput, SourceSetData> sourceSets = new LinkedHashMap<>();

    /**
     * Used to lookup {@link SourceSetData} instances from any of its originating file outputs
     * when parsing source set dependencies.
     */
    public final Map<File, SourceSetData> sourceSetOutputs = new LinkedHashMap<>();
}
