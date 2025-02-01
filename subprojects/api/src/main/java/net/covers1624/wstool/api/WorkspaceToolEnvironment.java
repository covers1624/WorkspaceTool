package net.covers1624.wstool.api;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Created by covers1624 on 17/5/23.
 */
public interface WorkspaceToolEnvironment {

    /**
     * @return The manifest file the wrapper launched.
     */
    @Nullable
    Path manifestFile();

    /**
     * @return The global system directory for Workspace Tool.
     */
    Path systemFolder();

    /**
     * @return The root directory of the project.
     */
    Path projectRoot();

    /**
     * @return The cache directory inside the root project.
     */
    Path projectCache();

    static WorkspaceToolEnvironment of() {
        return of(
                getPathProperty("wstool.manifest"),
                Path.of(System.getProperty("user.home"), ".workspace_tool"),
                Path.of(".").toAbsolutePath().normalize()
        );
    }

    static WorkspaceToolEnvironment of(@Nullable Path manifest, Path sysFolder, Path projectRoot) {
        return of(manifest, sysFolder, projectRoot, projectRoot.resolve(".wstool/"));
    }

    static WorkspaceToolEnvironment of(@Nullable Path manifest, Path sysFolder, Path projectRoot, Path projectCache) {
        // @formatter:off
        return new WorkspaceToolEnvironment() {
            @Nullable @Override public Path manifestFile() { return manifest; }
            @Override public Path systemFolder() { return sysFolder; }
            @Override public Path projectRoot() { return projectRoot; }
            @Override public Path projectCache() { return projectCache; }
        };
        // @formatter:on
    }

    @Nullable
    private static Path getPathProperty(String sysProp) {
        String val = System.getProperty(sysProp);
        if (val == null) return null;

        return Path.of(val);
    }
}
