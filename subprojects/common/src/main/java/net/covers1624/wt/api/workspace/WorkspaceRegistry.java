/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.Workspace;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by covers1624 on 23/7/19.
 */
public interface WorkspaceRegistry {

    /**
     * Registers the Api-Class and factory for its Implementation.
     * Almost exclusively called by Scripts to set things up.
     *
     * @param apiClazz The Api-Class.
     * @param factory  The factory to construct the Api-Classes Implementation.
     */
    <T extends Workspace> void registerScriptImpl(Class<T> apiClazz, Function<MixinInstantiator, T> factory);

    /**
     * Called to invoke the factory associated with the supplied Api-Class.
     *
     * @param apiClazz          The Api-Class.
     * @param mixinInstantiator The MixinInstantiator.
     * @return An instance of the supplied Api-Classes implementation.
     */
    <T extends Workspace> T constructScriptImpl(Class<T> apiClazz, MixinInstantiator mixinInstantiator);

    <T extends Workspace> void registerWorkspaceHandler(Class<T> apiClazz, Supplier<WorkspaceHandler<T>> factory);

    <T extends Workspace> WorkspaceHandler<T> constructWorkspaceHandlerImpl(Class<T> apiClazz);

    /**
     * Registers a {@link WorkspaceWriter} for the provided {@link Workspace} Api class.
     *
     * @param apiClazz The Api-Class.
     * @param factory  The Factory to construct the {@link WorkspaceWriter}
     */
    <T extends Workspace> void registerWorkspaceWriter(Class<T> apiClazz, Function<WorkspaceToolContext, WorkspaceWriter<T>> factory);

    /**
     * Called to construct a {@link WorkspaceWriter} for the given Api-Class.
     *
     * @param apiClazz The Api-Class.
     * @param context  The Context
     * @return The new {@link WorkspaceWriter}
     */
    <T extends Workspace> WorkspaceWriter<T> getWorkspaceWriter(Class<T> apiClazz, WorkspaceToolContext context);
}
