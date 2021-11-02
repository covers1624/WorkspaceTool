/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script;

import net.covers1624.wt.api.script.ScriptDepsSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 3/8/19.
 */
public class DefaultScriptDepsContainer implements ScriptDepsSpec {

    private final List<String> repos = new ArrayList<>();
    private final List<String> classpathDeps = new ArrayList<>();

    @Override
    public void repo(String repoName) {
        repos.add(repoName);
    }

    @Override
    public void classpath(String name) {
        classpathDeps.add(name);
    }

    public List<String> getRepos() {
        return repos;
    }

    public List<String> getClasspathDeps() {
        return classpathDeps;
    }
}
