package net.covers1624.wstool.intellij;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.extension.WorkspaceType;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.intellij.workspace.IJWorkspace;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by covers1624 on 21/10/24.
 */
public record IntellijWorkspaceType(
        @Nullable List<RunConfigTemplate> runs
) implements WorkspaceType {

    @Override
    public List<RunConfigTemplate> runs() {
        return runs != null ? runs : List.of();
    }

    @Override
    public Workspace newWorkspace(Environment env) {
        return new IJWorkspace(env);
    }
}
