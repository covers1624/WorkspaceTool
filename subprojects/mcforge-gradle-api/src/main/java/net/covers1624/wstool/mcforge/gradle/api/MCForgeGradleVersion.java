package net.covers1624.wstool.mcforge.gradle.api;

import net.covers1624.wstool.gradle.api.data.Data;

/**
 * Created by covers1624 on 1/12/25.
 */
public class MCForgeGradleVersion extends Data {

    public final String version;

    public MCForgeGradleVersion(String version) {
        this.version = version;
    }
}
