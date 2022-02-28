/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.api.gradle.data.ExtraData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 31/05/19.
 */
@VersionedClass (1)
public class FG2Data implements ExtraData {

    public String mcpMappings;
    public String mcVersion;
    public String forgeVersion;

    public List<String> fmlCoreMods = new ArrayList<>();
    public List<String> tweakClasses = new ArrayList<>();
}
