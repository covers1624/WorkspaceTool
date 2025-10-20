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
        registry.registerFrameworkType("neoforge:1.21", NeoForge_1_21_1_FrameworkType.class);
        registry.registerFrameworkType("neoforge:1.21.1", NeoForge_1_21_1_FrameworkType.class);
        registry.registerFrameworkType("neoforge:1.21.4", NeoForge_1_21_4_FrameworkType.class);
        registry.registerFrameworkType("neoforge:1.21.10", NeoForge_1_21_10_FrameworkType.class);
    }
}
