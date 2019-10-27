package net.covers1624.wt.api.data;

import net.covers1624.wt.event.VersionedClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data model for data extracted from Gradle.
 *
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (2)
public class ProjectData extends ExtraDataExtensible {

    public PluginData pluginData;

    public String name;
    public String rootProject;

    public String version;
    public String group;
    public String archivesBaseName;

    public Map<String, String> extraProperties = new HashMap<>();

    public List<ProjectData> subProjects = new ArrayList<>();

    public Map<String, ConfigurationData> configurations = new HashMap<>();
    public Map<String, SourceSetData> sourceSets = new HashMap<>();
}
