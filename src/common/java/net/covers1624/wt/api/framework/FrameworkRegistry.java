package net.covers1624.wt.api.framework;

import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.GradleModelCache;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * A Registry where various Frameworks can be registered to WorkspaceTool.
 *
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
     * @param handler  The Factory to construct the {@link FrameworkHandler}
     */
    <T extends ModdingFramework> void registerFrameworkHandler(Class<T> apiClass, FrameworkHandlerFactory<T> handler);

    /**
     * Called to construct the {@link FrameworkHandler} associated with supplied the Api-Class.
     *
     * @param apiClazz      The Api-Class.
     * @param projectDir    The Path to the root of the Project.
     * @param cacheDir      The Path to WorkspaceTools' Cache directory.
     * @param gradleManager An instance of {@link GradleManager}.
     * @param modelCache    An instance of {@link GradleModelCache}
     * @return The {@link FrameworkHandler} instance..
     */
    <T extends ModdingFramework> FrameworkHandler<T> getFrameworkHandler(Class<T> apiClazz, Path projectDir, Path cacheDir, GradleManager gradleManager, GradleModelCache modelCache);

    /**
     * A Factory to construct {@link FrameworkHandler} instances.
     */
    interface FrameworkHandlerFactory<T extends ModdingFramework> {

        /**
         * See {@link FrameworkRegistry#getFrameworkHandler(Class, Path, Path, GradleManager, GradleModelCache)}
         */
        FrameworkHandler<T> create(Path workspaceDir, Path projectDir, GradleManager gradleManager, GradleModelCache modelCache);
    }

}
