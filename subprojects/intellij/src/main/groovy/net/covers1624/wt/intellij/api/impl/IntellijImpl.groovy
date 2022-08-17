/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.api.impl


import net.covers1624.wt.api.impl.workspace.AbstractWorkspace
import net.covers1624.wt.api.mixin.MixinInstantiator
import net.covers1624.wt.intellij.api.script.Intellij

/**
 * Created by covers1624 on 23/7/19.
 */
class IntellijImpl extends AbstractWorkspace implements Intellij {

    private String jdkName
    private List<String> excludeDirs = new ArrayList<>()

    IntellijImpl(MixinInstantiator mixinInstantiator) {
        super(mixinInstantiator)
    }

    @Override
    void setJdkName(String name) {
        jdkName = name
    }

    @Override
    String getJdkName() {
        return jdkName
    }

    @Override
    void excludeDir(String path) {
        excludeDirs.add(path)
    }

    @Override
    List<String> getExcludeDirs() {
        return excludeDirs
    }
}