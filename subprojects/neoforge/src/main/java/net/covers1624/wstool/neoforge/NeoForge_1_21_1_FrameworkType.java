package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.gradle.api.data.ProjectExtData;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge_1_21_1_FrameworkType(
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
        if (run.mainClass() == null) {
            run.mainClass("cpw.mods.bootstraplauncher.BootstrapLauncher");
        }

        switch (type) {
            case "client" -> {
                run.args().addAll("--launchTarget", "forgeclientdev");
                run.args().addAll(
                        "--version", gradleData.version,
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.fmlVersion", properties.get("fancy_mod_loader_version"),
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
            }
            case "data" -> {
                run.args().addAll("--launchTarget", "forgedatadev");
                run.args().addAll("--gameDir", "."); //Patch states that this is 'required' by ModLauncher, but it falls back to this..
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.fmlVersion", properties.get("fancy_mod_loader_version"),
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
            }
            case "server" -> {
                run.args().addAll("--launchTarget", "forgeserverdev");
                run.args().addAll(
                        "--fml.neoForgeVersion", gradleData.version,
                        "--fml.fmlVersion", properties.get("fancy_mod_loader_version"),
                        "--fml.mcVersion", properties.get("minecraft_version"),
                        "--fml.neoFormVersion", properties.get("neoform_version")
                );
            }
            default -> throw new RuntimeException("Unknown type. Expecting 'client', 'server', 'data'. Got: " + type);
        }

        run.vmArgs().addAll(
                "--add-modules", "ALL-MODULE-PATH",
                "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
                "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming"
        );
    }
}
