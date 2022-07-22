/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.api.script;

import net.covers1624.wt.api.script.runconfig.RunConfig;

/**
 * This is runtime mixed into {@link RunConfig}
 * <p>
 * Created by covers1624 on 8/8/19.
 */
public interface IntellijRunConfig extends RunConfig {

    /**
     * Sets the Classpath Module for Intellij run configs.
     *
     * @param classpathModule The module name.
     */
    default void classpathModule(String classpathModule) {
        setClasspathModule(classpathModule);
    }

    /**
     * Sets the Classpath Module for Intellij run configs.
     *
     * @param classpathModule The module name.
     */
    void setClasspathModule(String classpathModule);

    /**
     * @return The Classpath Module name.
     */
    String getClasspathModule();

    /**
     * Enables Intellij's classpath shortening.
     */
    default void withShortClasspath() {
        setClasspathShortening(true);
    }

    /**
     * Sets weather classpath shortening is enabled or not.
     *
     * @param bool The value.
     */
    void setClasspathShortening(boolean bool);

    /**
     * Gets weather classpath shortening is enabled.
     *
     * @return If classpath shortening is enabled.
     */
    boolean getClasspathShortening();
}
