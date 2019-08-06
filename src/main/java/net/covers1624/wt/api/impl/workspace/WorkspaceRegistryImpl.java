package net.covers1624.wt.api.impl.workspace;

import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.workspace.Workspace;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import net.covers1624.wt.api.workspace.WorkspaceWriter;
import net.covers1624.wt.util.scala.ScalaSdk;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by covers1624 on 23/7/19.
 */
public class WorkspaceRegistryImpl implements WorkspaceRegistry {

    private Map<Class<? extends Workspace>, Supplier<? extends Workspace>> scriptFactories = new HashMap<>();
    private final Map<Class<? extends Workspace>, WorkspaceRegistry.WorkspaceWriterFactory<?>> writerFactories = new HashMap<>();

    @Override
    public <T extends Workspace> void registerScriptImpl(Class<T> apiClazz, Supplier<T> factory) {
        scriptFactories.put(apiClazz, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> T constructScriptImpl(Class<T> apiClazz) {
        Supplier<T> factory = (Supplier<T>) scriptFactories.get(apiClazz);
        if (factory == null) {
            throw new RuntimeException("No factory registered for type: " + apiClazz);
        }
        return factory.get();
    }

    @Override
    public <T extends Workspace> void registerWorkspaceWriter(Class<T> apiClazz, WorkspaceWriterFactory<T> factory) {
        writerFactories.put(apiClazz, factory);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> WorkspaceWriter<T> getWorkspaceWriter(Class<T> apiClazz, Path projectDir, DependencyLibrary library, ScalaSdk scalaSdk) {
        WorkspaceWriterFactory<T> factory = (WorkspaceWriterFactory<T>) writerFactories.get(apiClazz);
        if (factory == null) {
            throw new IllegalArgumentException("No writer registered for type: " + apiClazz);
        }
        return factory.create(projectDir, library, scalaSdk);
    }

}
