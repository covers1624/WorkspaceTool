package net.covers1624.wt.forge.api.impl;

import net.covers1624.wt.forge.api.script.Forge114RunConfig;

/**
 * Created by covers1624 on 8/8/19.
 */
public abstract class Forge114RunConfigTemplate implements Forge114RunConfig {

    private String launchTarget;

    @Override
    public void setLaunchTarget(String launchTarget) {
        this.launchTarget = launchTarget;
    }

    @Override
    public String getLaunchTarget() {
        return launchTarget;
    }
}
