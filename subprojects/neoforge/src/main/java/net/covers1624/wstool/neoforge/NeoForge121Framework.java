package net.covers1624.wstool.neoforge;

import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge121Framework(
        @Nullable String path,
        @Nullable String url,
        @Nullable String branch,
        @Nullable String commit
) implements NeoForgeFramework {

    // @formatter:off
    @Override public String path() { return path != null ? path : "NeoForge"; }
    @Override public String url() { return url != null ? url : "https://github.com/neoforged/NeoForge.git"; }
    @Override public String branch() { return requireNonNull(branch, "NeoForge framework requires branch."); }
    @Override public String commit() { return requireNonNull(commit, "NeoForge framework requires commit."); }
    // @formatter:on
}
