/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.script.Workspace;

/**
 * Used to write the current Module model out to a Workspace for any IDE.
 * <p>
 * Created by covers1624 on 20/7/19.
 */
public interface WorkspaceWriter<T extends Workspace> {

    /**
     * Write out the workspace.
     *
     * @param frameworkImpl The Framework Impl provided from the script.
     */
    void write(T frameworkImpl);
}
