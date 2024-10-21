package net.covers1624.wstool.api.config;

import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.extension.Workspace;

import java.util.List;

/**
 * Created by covers1624 on 19/9/24.
 */
public record Config(
        List<Framework> frameworks,
        List<String> modules,
        Workspace workspace
) {
}
