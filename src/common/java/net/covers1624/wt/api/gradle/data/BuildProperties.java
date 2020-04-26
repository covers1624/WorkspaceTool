package net.covers1624.wt.api.gradle.data;

import net.covers1624.wt.api.gradle.GradleManager;

import java.util.Set;

/**
 * Passed to WorkspaceTool's ModelBuilder gradle side.
 *
 * @see GradleManager
 *
 * Created by covers1624 on 18/6/19.
 */
public interface BuildProperties {

    /**
     * @return Class names for extra DataBuilder's to use gradle side.
     */
    Set<String> getDataBuilders();

    void setDataBuilders(Set<String> dataBuilders);
}
