/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.dependency;

import net.covers1624.wt.api.module.Module;

/**
 * A library of dependencies. That's all really..
 *
 * Created by covers1624 on 20/7/19.
 */
public interface DependencyLibrary {

    /**
     * @return All the dependencies contained in this library.
     */
    Iterable<LibraryDependency> getDependencies();

    void consume(Module module);

    /**
     * Adds a MavenDependency to this library.
     * Either returns an existing LibraryDependency or creates a new one.
     *
     * @param mavenDep The MavenDependency to add.
     * @return
     */
    LibraryDependency resolve(MavenDependency mavenDep);

    LibraryDependency resolve(ScalaSdkDependency version);

}
