package net.covers1624.wt.gradle.builder;

import net.covers1624.wt.api.data.GradleData;
import net.covers1624.wt.api.data.PluginData;
import net.covers1624.wt.event.VersionedClass;
import org.gradle.api.Project;

/**
 * Created by covers1624 on 18/6/19.
 */
@VersionedClass (1)
public interface ExtraDataBuilder {

    void preGradleData(Project project, PluginData pluginData) throws Exception;

    void build(Project project, PluginData pluginData, GradleData gradleData) throws Exception;
}
