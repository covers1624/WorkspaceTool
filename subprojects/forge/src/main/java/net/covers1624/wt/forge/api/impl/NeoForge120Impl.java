package net.covers1624.wt.forge.api.impl;

import net.covers1624.wt.forge.api.script.NeoForge120;

/**
 * Created by covers1624 on 2/9/23.
 */
public class NeoForge120Impl extends AbstractForgeFrameworkImpl implements NeoForge120 {
    public NeoForge120Impl() {
        setPath("NeoForge");
        setBranch("1.20.x");
        setUrl("https://github.com/neoforged/NeoForge.git");
    }
}
