package net.covers1624.wt.api.gradle.model;

import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.data.PluginData;
import net.covers1624.wt.event.VersionedClass;

import java.io.Serializable;

/**
 * Data Model for data extracted from Gradle.
 *
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public interface WorkspaceToolModel extends Serializable {

    /**
     * @return the PluginData instance.
     */
    PluginData getPluginData();

    /**
     * @return the GradleData instance.
     */
    GradleData getGradleData();
}
