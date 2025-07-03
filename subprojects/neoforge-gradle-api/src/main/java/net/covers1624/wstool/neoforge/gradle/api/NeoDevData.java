package net.covers1624.wstool.neoforge.gradle.api;

import net.covers1624.wstool.gradle.api.data.Data;

/**
 * Created by covers1624 on 5/25/25.
 */
public class NeoDevData extends Data {

    public final String moduleClasspathConfiguration;

    public NeoDevData(String moduleClasspathConfiguration) {
        this.moduleClasspathConfiguration = moduleClasspathConfiguration;
    }
}
