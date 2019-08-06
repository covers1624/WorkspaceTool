package net.covers1624.wt.api;

/**
 * Represents some form of Extension to WorkspaceTool.
 *
 * Created by covers1624 on 17/6/19.
 */
public interface Extension {

    /**
     * Called at the beginning of the WorkspaceTool life cycle to initialize.
     */
    void load();
}
