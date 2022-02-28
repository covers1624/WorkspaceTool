/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.forge.gradle.data;

import net.covers1624.wt.api.gradle.data.ExtraData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 9/8/19.
 */
public class FG3Data implements ExtraData {

    public List<File> accessTransformers = new ArrayList<>();
    public List<File> sideAnnotationStrippers = new ArrayList<>();
}
