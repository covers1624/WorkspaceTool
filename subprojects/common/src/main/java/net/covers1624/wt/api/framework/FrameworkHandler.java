/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.framework;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.script.ModdingFramework;
import net.covers1624.wt.api.script.NullFramework;

/**
 * This is used to control the Framework API for the exported workspace.
 *
 * @see FrameworkRegistry
 * @see ModdingFramework
 * Created by covers1624 on 10/7/19.
 */
public interface FrameworkHandler<T extends ModdingFramework> {

    /**
     * Called to construct the Framework's Modules and set everything up.
     * In the case of Forge1.12, This would clone forge, find AT's and
     * setup the Forge module ready to go.
     * Any Modules generated by this framework should be added to {@link WorkspaceToolContext#frameworkModules}
     *
     * @param frameworkImpl The Framework's impl.
     */
    void constructFrameworkModules(T frameworkImpl);

    class NullFrameworkHandler implements FrameworkHandler<NullFramework> {

        public NullFrameworkHandler(WorkspaceToolContext ctx) {
        }

        @Override
        public void constructFrameworkModules(NullFramework frameworkImpl) {
        }
    }

}
