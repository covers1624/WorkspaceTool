package net.covers1624.wt.intellij.api.script

import net.covers1624.wt.api.script.Workspace

/**
 * Created by covers1624 on 23/7/19.
 */
interface Intellij extends Workspace {

    default void jdkName(String name) {
        setJdkName(name)
    }

    void setJdkName(String name)

    String getJdkName()
}
