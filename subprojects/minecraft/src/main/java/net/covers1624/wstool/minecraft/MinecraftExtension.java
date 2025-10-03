package net.covers1624.wstool.minecraft;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.extension.Extension;

/**
 * Created by covers1624 on 10/1/25.
 */
@Extension.Details (
        id = "minecraft",
        desc = "Provides loader-agnostic Minecraft support to other Extensions."
)
public class MinecraftExtension implements Extension {

    @Override
    public void prepareEnvironment(Environment env) {
        env.putService(AssetDownloader.class, new AssetDownloader(env));
        env.putService(JSTExecutor.class, new JSTExecutor(env));
    }
}
