/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.api.script

import net.covers1624.wt.api.script.Workspace

/**
 * Created by covers1624 on 23/7/19.
 */
trait Intellij implements Workspace {

    void jdkName(String name) {
        setJdkName(name)
    }

    abstract void setJdkName(String name)

    abstract String getJdkName()
}
