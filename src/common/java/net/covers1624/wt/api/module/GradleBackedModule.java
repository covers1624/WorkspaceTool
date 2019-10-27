package net.covers1624.wt.api.module;

import net.covers1624.wt.api.data.ProjectData;
import net.covers1624.wt.api.data.PluginData;

/**
 * Represents a Module that has been constructed from Gradle.
 *
 * Created by covers1624 on 1/7/19.
 */
public interface GradleBackedModule extends Module {

    /**
     * @return The {@link ProjectData} from gradle.
     */
    ProjectData getProjectData();

}
