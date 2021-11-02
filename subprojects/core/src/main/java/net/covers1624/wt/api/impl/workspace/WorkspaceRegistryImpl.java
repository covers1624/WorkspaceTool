/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.workspace;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.Workspace;
import net.covers1624.wt.api.workspace.WorkspaceHandler;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import net.covers1624.wt.api.workspace.WorkspaceWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by covers1624 on 23/7/19.
 */
public class WorkspaceRegistryImpl implements WorkspaceRegistry {

    private final Map<Class<? extends Workspace>, Function<MixinInstantiator, ? extends Workspace>> scriptFactories = new HashMap<>();
    private final Map<Class<? extends Workspace>, Supplier<? extends WorkspaceHandler>> handlerFactories = new HashMap<>();
    private final Map<Class<? extends Workspace>, Function<WorkspaceToolContext, ? extends WorkspaceWriter>> writerFactories = new HashMap<>();

    @Override
    public <T extends Workspace> void registerScriptImpl(Class<T> apiClazz, Function<MixinInstantiator, T> factory) {
        scriptFactories.put(apiClazz, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> T constructScriptImpl(Class<T> apiClazz, MixinInstantiator mixinInstantiator) {
        Function<MixinInstantiator, T> factory = (Function<MixinInstantiator, T>) scriptFactories.get(apiClazz);
        if (factory == null) {
            throw new RuntimeException("No factory registered for type: " + apiClazz);
        }
        return factory.apply(mixinInstantiator);
    }

    @Override
    public <T extends Workspace> void registerWorkspaceHandler(Class<T> apiClazz, Supplier<WorkspaceHandler<T>> factory) {
        handlerFactories.put(apiClazz, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> WorkspaceHandler<T> constructWorkspaceHandlerImpl(Class<T> apiClazz) {
        Supplier<WorkspaceHandler<T>> factory = (Supplier<WorkspaceHandler<T>>) handlerFactories.get(apiClazz);
        if (factory == null) {
            throw new RuntimeException("No factory registered for type: " + apiClazz);
        }
        return factory.get();
    }

    @Override
    public <T extends Workspace> void registerWorkspaceWriter(Class<T> apiClazz, Function<WorkspaceToolContext, WorkspaceWriter<T>> factory) {
        writerFactories.put(apiClazz, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> WorkspaceWriter<T> getWorkspaceWriter(Class<T> apiClazz, WorkspaceToolContext context) {
        Function<WorkspaceToolContext, ? extends WorkspaceWriter> factory = writerFactories.get(apiClazz);
        if (factory == null) {
            throw new IllegalArgumentException("No writer registered for type: " + apiClazz);
        }
        return factory.apply(context);
    }

}
