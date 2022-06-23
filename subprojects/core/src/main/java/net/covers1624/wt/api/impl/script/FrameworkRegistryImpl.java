/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.script.ModdingFramework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 13/05/19.
 */
public class FrameworkRegistryImpl implements FrameworkRegistry {

    private final Map<Class<? extends ModdingFramework>, Supplier<? extends ModdingFramework>> scriptImpls = new HashMap<>();
    private final Map<Class<? extends ModdingFramework>, Function<WorkspaceToolContext, ? extends FrameworkHandler<?>>> handlerImpls = new HashMap<>();

    @Override
    public <T extends ModdingFramework> void registerScriptImpl(Class<T> apiClass, Supplier<T> factory) {
        scriptImpls.put(apiClass, factory);
    }

    @Override
    public <T extends ModdingFramework> T constructScriptImpl(Class<T> clazz) {
        Supplier<?> factory = scriptImpls.get(clazz);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for type: " + clazz);
        }
        return unsafeCast(factory.get());
    }

    @Override
    public <T extends ModdingFramework> void registerFrameworkHandler(Class<T> apiClass, Function<WorkspaceToolContext, FrameworkHandler<T>> handler) {
        handlerImpls.put(apiClass, handler);
    }

    @Override
    public <T extends ModdingFramework> FrameworkHandler<T> getFrameworkHandler(Class<T> apiClazz, WorkspaceToolContext context) {
        Function<WorkspaceToolContext, ? extends FrameworkHandler<?>> frameworkHandler = handlerImpls.get(apiClazz);
        if (frameworkHandler == null) {
            throw new IllegalArgumentException("No handler registered for type: " + apiClazz);
        }
        return unsafeCast(frameworkHandler.apply(context));
    }
}
