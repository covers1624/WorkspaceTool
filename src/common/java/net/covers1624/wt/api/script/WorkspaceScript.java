package net.covers1624.wt.api.script;

import groovy.lang.Closure;
import net.covers1624.wt.api.framework.ModdingFramework;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.runconfig.RunConfigContainer;
import net.covers1624.wt.api.workspace.Workspace;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * WorkspaceTool scripts extend from this class.
 * Most methods are named so Groovy Magic can happen.
 *
 * Created by covers1624 on 13/05/19.
 */
public interface WorkspaceScript {

    /**
     * Gets a path.
     *
     * @param str The str.
     * @return The Path.
     */
    Path path(String str);

    //    default void scriptDeps(Closure<ScriptDepsSpec> closure) {
    //        scriptDeps(new ClosureBackedConsumer<>(closure));
    //    }
    //
    //    void scriptDeps(Consumer<ScriptDepsSpec> consume);

    /**
     * Defines the use of a specified Framework.
     * Closure provided is used to configure the Framework.
     *
     * @param clazz   The Api-Class for the Framework.
     * @param closure The Configure closure.
     */
    default <T extends ModdingFramework> void framework(Class<T> clazz, Closure<T> closure) {
        framework(clazz, new ClosureBackedConsumer<>(closure));
    }

    /**
     * Overload of {@link #framework(Class, Closure)}.
     */
    <T extends ModdingFramework> void framework(Class<T> clazz, Consumer<T> consumer);

    /**
     * Adds a Dependency override.
     * Useful to force some maven dependencies to have specific group's or modules.
     * For Example: jei uses 'mezz.jei:jei_1.12' and 'mezz.jei:jei_1.12.2'
     * With this we can override 'jei_1.12' with jei '1.12.2'
     *
     * @param overrides The overrides.
     */
    void depOverride(Map<String, String> overrides);

    /**
     * Overload of {@link #depOverride(Map)}
     */
    void depOverride(String from, String to);

    /**
     * Configures the ModuleContainer.
     *
     * @param closure The Closure.
     */
    default void modules(Closure<ModuleContainerSpec> closure) {
        modules(new ClosureBackedConsumer<>(closure));
    }

    /**
     * Overload of {@link #modules(Closure)}
     */
    void modules(Consumer<ModuleContainerSpec> consumer);

    /**
     * Defines what Workspace api to use.
     * Closure is provided to configure the Workspace.
     *
     * @param clazz   The Api-Class for the Workspace.
     * @param closure The configure Closure.
     */
    default <T extends Workspace> void workspace(Class<T> clazz, Closure<T> closure) {
        workspace(clazz, new ClosureBackedConsumer<>(closure));
    }

    /**
     * Overload of {@link #workspace(Class, Closure)}
     */
    <T extends Workspace> void workspace(Class<T> clazz, Consumer<T> consumer);

    /**
     * Overload of {@link #workspace(Class, Closure)} with no configure block.
     */
    <T extends Workspace> void workspace(Class<T> clazz);

    /**
     * Configures the RunConfigContainer.
     *
     * @param closure The configure Closure.
     */
    default void runConfigs(Closure<RunConfigContainer> closure) {
        runConfigs(new ClosureBackedConsumer<>(closure));
    }

    /**
     * Overload of {@link #runConfigs(Closure)}
     */
    void runConfigs(Consumer<RunConfigContainer> consumer);

    /**
     * @return Gets the RunConfigContainer.
     */
    RunConfigContainer getRunConfigContainer();

}
/*Iggy was here*/
