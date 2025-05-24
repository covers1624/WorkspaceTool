package net.covers1624.wstool.api.extension;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.workspace.Workspace;

import java.util.List;

/**
 * A workspace, the output of a WorkspaceTool project.
 * <p>
 * Created by covers1624 on 20/10/24.
 */
public interface WorkspaceType {

    List<RunConfigTemplate> runs();

    /**
     * Create a new {@link Workspace}.
     *
     * @param env The environment.
     * @return The new builder.
     */
    Workspace newWorkspace(Environment env);
}
