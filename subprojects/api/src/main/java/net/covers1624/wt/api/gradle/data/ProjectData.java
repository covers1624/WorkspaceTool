/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.data;

import com.google.common.collect.Streams;
import net.covers1624.wt.api.event.VersionedClass;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Data model for data extracted from Gradle.
 * <p>
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (2)
public class ProjectData extends ExtraDataExtensible {

    public PluginData pluginData;

    public String name;
    @Nullable
    public String parent;
    public File projectDir;

    public String version;
    public String group;
    public String archivesBaseName;

    public Map<String, String> extraProperties = new HashMap<>();

    public Map<String, ProjectData> subProjects = new HashMap<>();

    public Map<String, ConfigurationData> configurations = new HashMap<>();
    public Map<String, SourceSetData> sourceSets = new HashMap<>();

    public String getProjectCoords() {
        if (parent != null) {
            return parent + ":" + name;
        }
        return name;
    }

    public Stream<ProjectData> streamAllProjects() {
        return Streams.concat(Stream.of(this), subProjects.values().stream().flatMap(ProjectData::streamAllProjects));
    }
}
