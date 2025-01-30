package net.covers1624.wstool.neoforge.gradle.api;

import net.covers1624.wstool.gradle.api.data.Data;

/**
 * Created by covers1624 on 1/30/25.
 */
public class NeoForgeGradleVersion extends Data {

    public final Variant variant;
    public final String version;

    public NeoForgeGradleVersion(Variant variant, String version) {
        this.variant = variant;
        this.version = version;
    }

    public enum Variant {
        NEO_GRADLE,
        MOD_DEV_GRADLE,
    }
}
