package net.covers1624.wstool.intellij;

import net.covers1624.wstool.api.config.RunConfig;
import net.covers1624.wstool.api.extension.Workspace;

import java.util.List;

/**
 * Created by covers1624 on 21/10/24.
 */
public record IntellijWorkspace(
        List<RunConfig> runs
) implements Workspace {
}
