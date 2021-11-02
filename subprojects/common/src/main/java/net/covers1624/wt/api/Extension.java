/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api;

/**
 * Represents some form of Extension to WorkspaceTool.
 *
 * Created by covers1624 on 17/6/19.
 */
public interface Extension {

    /**
     * Called at the beginning of the WorkspaceTool life cycle to initialize.
     */
    void load();
}
