/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.data;

import net.covers1624.wt.event.VersionedClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data model for plugins loaded on a Gradle instance.
 *
 * Created by covers1624 on 1/06/19.
 */
@VersionedClass (1)
public class PluginData extends ExtraDataExtensible {

    public final Set<String> pluginIds = new HashSet<>();
    public final Set<String> pluginClasses = new HashSet<>();
    public final Map<String, String> classToName = new HashMap<>();
}
