/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.gradle.data.ExtraData;
import net.covers1624.wt.event.VersionedClass;
import net.covers1624.wt.forge.gradle.FGVersion;

/**
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public class FGPluginData implements ExtraData {

    public String versionString;
    public FGVersion version;
}
