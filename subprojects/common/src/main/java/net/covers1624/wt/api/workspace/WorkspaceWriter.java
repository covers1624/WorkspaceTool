package net.covers1624.wt.api.workspace;

import net.covers1624.wt.api.script.Workspace;

/**
 * Used to write the current Module model out to a Workspace for any IDE.
 *
 * Created by covers1624 on 20/7/19.
 */
public interface WorkspaceWriter<T extends Workspace> {

    /**
     * Write out the workspace.
     *
     * @param frameworkImpl The Framework Impl provided from the script.
     */
    void write(T frameworkImpl);
}
