package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.util.scala.ScalaSdk;

import java.nio.file.Path;
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
    <T extends Workspace> void registerScriptImpl(Class<T> apiClazz, Supplier<T> factory);

    /**
     * Called to invoke the factory associated with the supplied Api-Class.
     *
     * @param apiClazz The Api-Class.
     * @return An instance of the supplied Api-Classes implementation.
     */
    <T extends Workspace> T constructScriptImpl(Class<T> apiClazz);

    /**
     * Registers a {@link WorkspaceWriter} for the provided {@link Workspace} Api class.
     *
     * @param apiClazz The Api-Class.
     * @param factory  The Factory to construct the {@link WorkspaceWriter}
     */
    <T extends Workspace> void registerWorkspaceWriter(Class<T> apiClazz, WorkspaceWriterFactory<T> factory);

    /**
     * Called to construct a {@link WorkspaceWriter} for the given Api-Class.
     *
     * @param apiClazz   The Api-Class.
     * @param projectDir The Project root directory.
     * @param library    The {@link DependencyLibrary}.
     * @param scalaSdk   The {@link ScalaSdk}.
     * @return The new {@link WorkspaceWriter}
     */
    <T extends Workspace> WorkspaceWriter<T> getWorkspaceWriter(Class<T> apiClazz, Path projectDir, DependencyLibrary library, ScalaSdk scalaSdk);

    /**
     * A factory to construct {@link WorkspaceWriter} instances.
     */
    interface WorkspaceWriterFactory<T extends Workspace> {

        /**
         * See {@link #getWorkspaceWriter(Class, Path, DependencyLibrary, ScalaSdk)}
         */
        WorkspaceWriter<T> create(Path projectDir, DependencyLibrary library, ScalaSdk scalaSdk);
    }

}
