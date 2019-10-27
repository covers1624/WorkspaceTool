package net.covers1624.wt.api.script;

import groovy.lang.Closure;
import net.covers1624.wt.api.script.runconfig.RunConfigContainer;
import net.covers1624.wt.util.ClosureBackedConsumer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 23/7/19.
 */
public interface Workspace {

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
