/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.event.VersionedClass;

import java.io.File;

/**
 * Created by covers1624 on 9/8/19.
 */
@VersionedClass (1)
public class FG3RunData {

    public String name;
    public String assetIndex;
    public File assetsDirectory;
    public File nativesDirectory;

}
