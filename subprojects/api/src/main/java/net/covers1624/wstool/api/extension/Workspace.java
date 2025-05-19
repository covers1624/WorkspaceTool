package net.covers1624.wstool.api.extension;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.module.WorkspaceBuilder;

import java.util.List;

/**
 * A workspace, the output of a WorkspaceTool project.
 * <p>
 * Created by covers1624 on 20/10/24.
 */
public interface Workspace {

    List<RunConfigTemplate> runs();

    /**
     * Create a new {@link WorkspaceBuilder}.
     *
     * @param env The environment.
     * @return The new builder.
     */
    WorkspaceBuilder builder(Environment env);
}
