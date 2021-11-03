/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.dependency;

/**
 * Represents a dependency.
 * <p>
 * Created by covers1624 on 30/6/19.
 */
public interface Dependency extends Cloneable {

    /**
     * This is somewhat specific to Intellij, some IDE's may not have this functionality.
     * When Module - Module dependencies are used in Intellij dependencies have the option
     * to 'export' their self down the chain.
     * By default all dependencies are exported.
     *
     * @return If the dependency should be exported by modules.
     */
    boolean getExport();

    /**
     * Set whether the dependency should be exported down the hierarchy.
     *
     * @param value the value.
     * @return The same Dependency object.
     */
    Dependency setExport(boolean value);

    /**
     * Copies this Dependency object.
     *
     * @return The copy.
     */
    Dependency copy();
}
