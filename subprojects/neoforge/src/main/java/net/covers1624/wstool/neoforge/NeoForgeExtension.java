package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.extension.Extension;

/**
 * Created by covers1624 on 21/10/24.
 */
@Extension.Details (
        id = "neoforge",
        desc = "Integration for NeoForge Gradle projects."
)
public class NeoForgeExtension implements Extension {

    @Override
    public void registerConfigTypes(ConfigTypeRegistry registry) {
        registry.registerFramework("neoforge:1.20", NeoForge120Framework.class);
        registry.registerFramework("neoforge:1.21", NeoForge121Framework.class);
    }
}
