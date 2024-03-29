/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.data;

import net.covers1624.wt.api.event.VersionedClass;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data model for a SourceSet.
 * <p>
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (1)
public class SourceSetData implements Serializable {

    public String name;
    public List<File> resources = new ArrayList<>();
    public Map<String, List<File>> sourceMap = new HashMap<>();

    public String compileConfiguration;
    public String runtimeConfiguration;
    public String compileOnlyConfiguration;

    public List<File> getOrComputeSrc(String name) {
        return sourceMap.computeIfAbsent(name, e -> new ArrayList<>());
    }
}
