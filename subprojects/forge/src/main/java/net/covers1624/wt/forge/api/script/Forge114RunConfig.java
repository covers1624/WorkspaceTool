package net.covers1624.wt.forge.api.script;

import net.covers1624.wt.api.script.runconfig.RunConfig;

/**
 * This is runtime mixed into {@link RunConfig}
 *
 * Created by covers1624 on 8/8/19.
 */
public interface Forge114RunConfig extends RunConfig {

    /**
     * Sets the FML Launch target.
     *
     * @param launchTarget The target.
     */
    default void launchTarget(String launchTarget) {
        setLaunchTarget(launchTarget);
    }

    /**
     * Sets the FML Launch target.
     *
     * @param launchTarget The target.
     */
    void setLaunchTarget(String launchTarget);

    /**
     * @return The FML Launch target.
     */
    String getLaunchTarget();

}
