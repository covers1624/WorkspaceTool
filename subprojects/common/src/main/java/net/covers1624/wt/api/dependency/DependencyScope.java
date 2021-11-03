/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.dependency;

import java.io.Serializable;

/**
 * Represents the basic scopes provided by Intellij.
 * <p>
 * Created by covers1624 on 8/01/19.
 */
public enum DependencyScope implements Serializable {
    PROVIDED,
    COMPILE,
    RUNTIME,
    TEST
}
