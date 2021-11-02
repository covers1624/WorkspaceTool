/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.data;

import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.event.VersionedClass;

import java.io.Serializable;

/**
 * Super interface for all data extracted by ExtraModelBuilders
 *
 * @see GradleManager
 *
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (1)
public interface ExtraData extends Serializable {}
