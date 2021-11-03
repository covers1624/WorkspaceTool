/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.framework;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.script.ModdingFramework;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Registry where various Frameworks can be registered to WorkspaceTool.
 * <p>
 * Created by covers1624 on 13/05/19.
 */
public interface FrameworkRegistry {

    /**
     * Registers the Api-Class and factory for its Implementation.
     * Almost exclusively called by Scripts to set things up.
     *
     * @param apiClass The Api-Class.
     * @param factory  The factory to construct the Api-Classes Implementation.
     */
    <T extends ModdingFramework> void registerScriptImpl(Class<T> apiClass, Supplier<T> factory);

    /**
     * Called to invoke the factory associated with the supplied Api-Class.
     *
     * @param clazz The Api-Class.
     * @return An instance of the supplied Api-Classes implementation.
     */
    <T extends ModdingFramework> T constructScriptImpl(Class<T> clazz);

    /**
     * Registers the Api-Class to the supplied FrameworkHandlerFactory.
     *
     * @param apiClass The Api-Class
     * @param factory  The Factory to construct the {@link FrameworkHandler}
     */
    <T extends ModdingFramework> void registerFrameworkHandler(Class<T> apiClass, Function<WorkspaceToolContext, FrameworkHandler<T>> factory);

    /**
     * Called to construct the {@link FrameworkHandler} associated with supplied the Api-Class.
     *
     * @param apiClazz The Api-Class.
     * @return The {@link FrameworkHandler} instance..
     */
    <T extends ModdingFramework> FrameworkHandler<T> getFrameworkHandler(Class<T> apiClazz, WorkspaceToolContext context);
}
