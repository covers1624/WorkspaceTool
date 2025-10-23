package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.workspace.runs.RunConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge_1_21_4_FrameworkType(
        @Nullable String path,
        @Nullable String url,
        @Nullable String branch,
        @Nullable String commit,
        @Nullable String parchment
) implements NeoForgeFrameworkType {

    // @formatter:off
    @Override public String path() { return path != null ? path : "NeoForge"; }
    @Override public String url() { return url != null ? url : "https://github.com/neoforged/NeoForge.git"; }
    @Override public String branch() { return requireNonNull(branch, "NeoForge framework requires branch."); }
    @Override public String commit() { return requireNonNull(commit, "NeoForge framework requires commit."); }
    // @formatter:on

    @Override
    public void addLaunchTarget(RunConfig run, String type) {
        if (run.mainClass() == null) {
            run.mainClass("cpw.mods.bootstraplauncher.BootstrapLauncher");
        }
        var launchTarget = switch (type) {
            case "client" -> "neoforgeclientdev";
            case "client_data" -> "neoforgeclientdatadev";
            case "server" -> "neoforgeserverdev";
            case "server_data" -> "neoforgeserverdatadev";
            default -> throw new RuntimeException("Unknown type. Expecting 'client', 'client_data', 'server', 'server_data'. Got: " + type);
        };
        if (launchTarget != null) {
            run.args().addAll(List.of("--launchTarget", launchTarget));
        }
    }
}
