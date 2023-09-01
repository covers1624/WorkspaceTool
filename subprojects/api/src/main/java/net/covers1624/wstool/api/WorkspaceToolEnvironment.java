package net.covers1624.wstool.api;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Created by covers1624 on 17/5/23.
 */
public final class WorkspaceToolEnvironment {

    /**
     * The Path to the manifest file the wrapper launched.
     */
    @Nullable
    public static final Path WSTOOL_MANIFEST = getPathProperty("wstool.manifest");

    @Nullable
    private static Path getPathProperty(String sysProp) {
        String val = System.getProperty(sysProp);
        if (val == null) return null;

        return Path.of(val);
    }
}
