/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script;

/**
 * Created by covers1624 on 3/8/19.
 */
public interface ScriptDepsSpec {

    void repo(String repoName);

    void classpath(String name);
}
