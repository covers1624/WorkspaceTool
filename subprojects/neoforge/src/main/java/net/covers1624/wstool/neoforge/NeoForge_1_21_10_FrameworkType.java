package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.gradle.api.data.ProjectExtData;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge_1_21_10_FrameworkType(
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
    public void addRunArguments(Module nfModule, Module nfSubModule, RunConfig run, String type) {
        var gradleData = requireNonNull(nfSubModule.projectData());
        var properties = requireNonNull(gradleData.getData(ProjectExtData.class))
                .properties;

        switch (type) {
            case "client" -> {
                run.args().addAll(
                        "--version", gradleData.version,
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
                run.mainClassIfMissing("net.neoforged.fml.startup.Client");
            }
            case "client_data" -> {
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
                run.mainClassIfMissing("net.neoforged.fml.startup.DataClient");
            }
            case "server" -> {
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
                run.mainClassIfMissing("net.neoforged.fml.startup.Server");
            }
            case "server_data" -> {
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
                run.mainClassIfMissing("net.neoforged.fml.startup.DataServer");
            }
            case "game_test_server" -> {
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
                run.mainClassIfMissing("net.neoforged.fml.startup.GameTestServer");
            }
            default -> throw new RuntimeException("Unknown type. Expecting 'client', 'client_data', 'server', 'server_data'. Got: " + type);
        }

        run.vmArgs().addAll(
                "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming"
        );
    }
}
