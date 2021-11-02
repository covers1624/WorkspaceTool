/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.intellij.api.workspace

import net.covers1624.wt.api.workspace.WorkspaceModule
import org.apache.commons.lang3.StringUtils

/**
 * Created by covers1624 on 3/9/19.
 */
interface IJWorkspaceModule extends WorkspaceModule {

    IJWorkspaceModule getParent()

    List<IJWorkspaceModule> getChildren()

    String getSourceSetName()

}
