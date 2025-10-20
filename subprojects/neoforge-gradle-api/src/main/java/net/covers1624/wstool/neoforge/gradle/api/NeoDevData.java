package net.covers1624.wstool.neoforge.gradle.api;

import net.covers1624.wstool.gradle.api.data.Data;
import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 5/25/25.
 */
public class NeoDevData extends Data {

    public final @Nullable String moduleClasspathConfiguration;

    public NeoDevData(@Nullable String moduleClasspathConfiguration) {
        this.moduleClasspathConfiguration = moduleClasspathConfiguration;
    }
}
