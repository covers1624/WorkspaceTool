package net.covers1624.wt.mc;

import net.covers1624.wt.api.Extension;
import net.covers1624.wt.api.ExtensionDetails;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.event.InitializationEvent;

/**
 * Created by covers1624 on 9/8/19.
 */
@ExtensionDetails (name = "Minecraft", desc = "Provides some minecraft stuff for other modules.")
public class MinecraftExtension implements Extension {

    @Override
    public void load() {
        InitializationEvent.REGISTRY.register(event -> {
            GradleManager gradleManager = event.getContext().gradleManager;
            //Provide all of us to model builders.
            gradleManager.includeClassMarker(MinecraftExtension.class);
        });
    }
}
