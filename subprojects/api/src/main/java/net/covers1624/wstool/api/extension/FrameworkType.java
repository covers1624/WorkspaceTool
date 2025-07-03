package net.covers1624.wstool.api.extension;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.workspace.Workspace;

/**
 * Created by covers1624 on 20/10/24.
 */
public interface FrameworkType {

    /**
     * Build the framework and its modules.
     * <p>
     * Building a framework may include anything, cloning Git repos, adding
     * dependencies to projects, etc.
     *
     * @param env             The {@link Environment}.
     * @param moduleProcessor The {@link ModuleProcessor} to create modules.
     * @param workspace       The {@link Workspace}.
     */
    void buildFrameworks(
            Environment env,
            ModuleProcessor moduleProcessor,
            Workspace workspace
    );
}
