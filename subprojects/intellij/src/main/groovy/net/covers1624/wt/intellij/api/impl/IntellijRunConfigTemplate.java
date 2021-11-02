package net.covers1624.wt.intellij.api.impl;

import net.covers1624.wt.intellij.api.script.IntellijRunConfig;

/**
 * Created by covers1624 on 8/8/19.
 */
public abstract class IntellijRunConfigTemplate implements IntellijRunConfig {

    private String classpathModule;

    @Override
    public void setClasspathModule(String classpathModule) {
        this.classpathModule = classpathModule;
    }

    @Override
    public String getClasspathModule() {
        return classpathModule;
    }
}
