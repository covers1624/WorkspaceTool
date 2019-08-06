package net.covers1624.wt.api.impl.script;

import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.framework.ModdingFramework;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.GradleModelCache;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.covers1624.wt.util.Utils.unsafeCast;

/**
 * Created by covers1624 on 13/05/19.
 */
public class FrameworkRegistryImpl implements FrameworkRegistry {

    private Map<Class<? extends ModdingFramework>, Supplier<? extends ModdingFramework>> scriptImpls = new HashMap<>();
    private final Map<Class<? extends ModdingFramework>, FrameworkHandlerFactory<?>> handlerImpls = new HashMap<>();

    @Override
    public <T extends ModdingFramework> void registerScriptImpl(Class<T> apiClass, Supplier<T> factory) {
        scriptImpls.put(apiClass, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends ModdingFramework> T constructScriptImpl(Class<T> clazz) {
        Supplier<T> factory = (Supplier<T>) scriptImpls.get(clazz);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for type: " + clazz);
        }
        return factory.get();
    }

    @Override
    public <T extends ModdingFramework> void registerFrameworkHandler(Class<T> apiClass, FrameworkHandlerFactory<T> handler) {
        handlerImpls.put(apiClass, handler);
    }

    @Override
    public <T extends ModdingFramework> FrameworkHandler<T> getFrameworkHandler(Class<T> apiClazz, Path projectDir, Path cacheDir, GradleManager gradleManager, GradleModelCache modelCache) {
        FrameworkHandlerFactory<?> frameworkHandler = handlerImpls.get(apiClazz);
        if (frameworkHandler == null) {
            throw new IllegalArgumentException("No handler registered for type: " + apiClazz);
        }
        return unsafeCast(frameworkHandler.create(projectDir, cacheDir, gradleManager, modelCache));
    }
}
