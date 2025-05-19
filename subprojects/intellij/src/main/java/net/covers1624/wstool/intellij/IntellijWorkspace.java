package net.covers1624.wstool.intellij;

import net.covers1624.wstool.api.config.RunConfigTemplate;
import net.covers1624.wstool.api.extension.Workspace;

import java.util.List;

/**
 * Created by covers1624 on 21/10/24.
 */
public record IntellijWorkspace(
        @Nullable List<RunConfigTemplate> runs
) implements Workspace {
}
