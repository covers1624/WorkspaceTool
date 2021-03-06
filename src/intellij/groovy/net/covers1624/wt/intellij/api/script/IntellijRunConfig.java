package net.covers1624.wt.intellij.api.script;

import net.covers1624.wt.api.script.runconfig.RunConfig;

/**
 * This is runtime mixed into {@link RunConfig}
 *
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

}
