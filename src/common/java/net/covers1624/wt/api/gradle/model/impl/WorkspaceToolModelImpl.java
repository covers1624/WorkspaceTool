package net.covers1624.wt.api.gradle.model.impl;

import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.data.PluginData;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.event.VersionedClass;

/**
 * An implementation of {@link WorkspaceToolModel}
 * {@inheritDoc}
 *
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public class WorkspaceToolModelImpl implements WorkspaceToolModel {

    private final PluginData pluginData;
    private final GradleData gradleData;

    public WorkspaceToolModelImpl(PluginData pluginData, GradleData gradleData) {
        this.pluginData = pluginData;
        this.gradleData = gradleData;
    }

    @Override
    public PluginData getPluginData() {
        return pluginData;
    }

    @Override
    public GradleData getGradleData() {
        return gradleData;
    }
}
