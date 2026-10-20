package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetAttribute;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

import java.io.File;
import java.util.*;

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
     * Back links from each configuration to the source sets they eventually reach.
     */
    public final Map<Configuration, List<SourceSet>> reachableSourceSets = new LinkedHashMap<>();

    /**
     * Any attributes to be applied to the extracted source set.
     */
    public final Map<SourceSet, Set<SourceSetAttribute>> sourceSetAttrs = new LinkedHashMap<>();

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

    public void addAttributes(SourceSet ss, Collection<SourceSetAttribute> attrs) {
        sourceSetAttrs.computeIfAbsent(ss, e -> new LinkedHashSet<>())
                .addAll(attrs);
    }

    public void addAttributes(Configuration config, Collection<SourceSetAttribute> attrs) {
        for (SourceSet ss : reachableSourceSets.getOrDefault(config, Collections.emptyList())) {
            addAttributes(ss, attrs);
        }
    }
}
