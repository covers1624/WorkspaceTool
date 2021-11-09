/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script.module;

import java.util.Set;

/**
 * Created by covers1624 on 23/05/19.
 */
public interface ModuleContainerSpec {

    void include(String... includes);

    Set<String> getIncludes();
}
