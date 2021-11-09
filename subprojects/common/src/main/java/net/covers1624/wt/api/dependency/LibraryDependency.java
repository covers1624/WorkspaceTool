/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.dependency;

/**
 * A Dependency that is stored in a {@link DependencyLibrary}
 * <p>
 * Created by covers1624 on 30/6/19.
 */
public interface LibraryDependency extends Dependency {

    /**
     * Gets the name of the library. Usually maven string.
     *
     * @return The name.
     */
    String getLibraryName();

    /**
     * Gets a file-safe name for the library.
     *
     * @return The name.
     */
    default String getLibraryFileName() {
        return getLibraryName().replaceAll("[:.-]", "_");
    }

    /**
     * Gets the underlying MavenDependency.
     *
     * @return The MavenDependency.
     */
    Dependency getDependency();

    /**
     * Sets the name for the library.
     *
     * @param name The name.
     * @return The same LibraryDependency.
     */
    LibraryDependency setLibraryName(String name);

    /**
     * Sets the underlying MavenDependency.
     *
     * @param dependency The MavenDependency.
     * @return The same LibraryDependency.
     */
    LibraryDependency setDependency(Dependency dependency);

    /**
     * {@inheritDoc}
     */
    @Override
    LibraryDependency setExport(boolean value);

    /**
     * {@inheritDoc}
     */
    @Override
    LibraryDependency copy();
}
