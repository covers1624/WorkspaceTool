package net.covers1624.wstool.intellij;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.extension.Workspace;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.intellij.module.IJWorkspaceBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by covers1624 on 21/10/24.
 */
public record IntellijWorkspace(
        @Nullable List<RunConfigTemplate> runs
) implements Workspace {

    @Override
    public List<RunConfigTemplate> runs() {
        return runs != null ? runs : List.of();
    }

    @Override
    public WorkspaceBuilder builder(Environment env) {
        return new IJWorkspaceBuilder(env);
    }
}
