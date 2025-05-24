package net.covers1624.wstool.intellij;

import net.covers1624.wstool.api.extension.Extension;

/**
 * Created by covers1624 on 20/10/24.
 */
@Extension.Details (
        id = "intellij",
        desc = "Intellij workspace generation support."
)
public class IntellijExtension implements Extension {

    @Override
    public void registerConfigTypes(ConfigTypeRegistry registry) {
        registry.registerWorkspaceType("intellij", IntellijWorkspaceType.class);
    }
}
