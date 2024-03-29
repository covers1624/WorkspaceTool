/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.script.Workspace;

/**
 * Created by covers1624 on 3/9/19.
 */
public interface WorkspaceHandler<T extends Workspace> {

    void buildWorkspaceModules(T workspace, WorkspaceToolContext context);

}
