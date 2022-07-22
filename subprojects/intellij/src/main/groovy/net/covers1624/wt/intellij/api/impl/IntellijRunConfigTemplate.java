/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.api.impl;

import net.covers1624.wt.intellij.api.script.IntellijRunConfig;

/**
 * Created by covers1624 on 8/8/19.
 */
public abstract class IntellijRunConfigTemplate implements IntellijRunConfig {

    private String classpathModule;
    private boolean classpathShortening;

    @Override
    public void setClasspathModule(String classpathModule) {
        this.classpathModule = classpathModule;
    }

    @Override
    public String getClasspathModule() {
        return classpathModule;
    }

    @Override
    public void setClasspathShortening(boolean value) {
        this.classpathShortening = value;
    }

    @Override
    public boolean getClasspathShortening() {
        return classpathShortening;
    }
}
