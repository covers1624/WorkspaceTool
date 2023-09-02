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

    /**
     * The Global system cache folder for Workspace Tool.
     */
    public static final Path WSTOOL_SYSTEM_FOLDER = Path.of(System.getProperty("user.home"), ".workspace_tool")
            .normalize()
            .toAbsolutePath();

    /**
     * The Global system cache folder for provisioned JDK's.
     */
    public static final Path WSTOOL_JDKS = WSTOOL_SYSTEM_FOLDER.resolve("jdks");

    @Nullable
    private static Path getPathProperty(String sysProp) {
        String val = System.getProperty(sysProp);
        if (val == null) return null;

        return Path.of(val);
    }
}
