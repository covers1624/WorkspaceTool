package net.covers1624.wstool.neoforge;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.gradle.api.data.ConfigurationData;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.ProjectExtData;
import net.covers1624.wstool.minecraft.SRGRemapper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForgeLegacy_1_20_1_FrameworkType(
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
    public Set<String> excludeModules() {
        var set = new HashSet<>(NeoForgeFrameworkType.super.excludeModules());
        set.add(":ForgeRoot:clean");
        set.add(":ForgeRoot:fmlonly");
        return set;
    }

    @Override
    public List<String> ideImportTasks(Environment env, Module nfModule, Module nfSubModule) {
        return List.of();
    }

    @Override
    public void postBuild(Environment env, Module nfModule, Module nfSubModule) {
        try {
            writeManifestEntries(nfModule.subModules().get("fmlcore"), Map.of(
                    "Automatic-Module-Name", "fmlcore"
//                    "FMLModType", "LIBRARY"
            ));
            writeManifestEntries(nfModule.subModules().get("fmlloader"), Map.of(
                    "Automatic-Module-Name", "fmlloader"
            ));
            writeManifestEntries(nfModule.subModules().get("fmlearlydisplay"), Map.of(
                    "Automatic-Module-Name", "fmlearlydisplay"
            ));
            writeManifestEntries(nfModule.subModules().get("javafmllanguage"), Map.of(
                    "Automatic-Module-Name", "javafmllanguage"
//                    "FMLModType", "LANGPROVIDER"
            ));
            writeManifestEntries(nfModule.subModules().get("lowcodelanguage"), Map.of(
                    "Automatic-Module-Name", "lowcodelanguage"
//                    "FMLModType", "LANGPROVIDER"
            ));
            writeManifestEntries(nfModule.subModules().get("mclanguage"), Map.of(
                    "Automatic-Module-Name", "mclanguage"
//                    "FMLModType", "LANGPROVIDER"
            ));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write files.", ex);
        }
    }

    private static void writeManifestEntries(@Nullable Module module, Map<String, String> entries) throws IOException {
        if (module == null) return;

        var manifestPath = module.rootDir().resolve("src/main/resources/META-INF/MANIFEST.MF");

        Manifest manifest = new Manifest();
        if (Files.exists(manifestPath)) {
            try (InputStream is = Files.newInputStream(manifestPath)) {
                manifest.read(is);
            }
        }
        if (!manifest.getMainAttributes().containsKey(Attributes.Name.MANIFEST_VERSION)) {
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }

        entries.forEach(manifest.getMainAttributes()::putValue);

        try (OutputStream os = Files.newOutputStream(IOUtils.makeParents(manifestPath))) {
            manifest.write(os);
            os.flush();
        }
    }

    @Override
    public Module getMainSubModule(Module nfModule) {
        var subModule = nfModule.subModules().get("neoforge");
        if (subModule == null) {
            subModule = nfModule.subModules().get("forge");
        }

        return subModule;
    }

    @Override
    public String getMcVersion(Module rootModule, Module module) {
        var rootGradleData = requireNonNull(rootModule.projectData());
        var rootProperties = requireNonNull(rootGradleData.getData(ProjectExtData.class))
                .properties;
        return requireNonNull(rootProperties.get("MC_VERSION"));
    }

    @Override
    public Map<String, String> getEnvVars(Module rootModule, Module nfSubModule) {
        var subprojectProperties = requireNonNull(requireNonNull(nfSubModule.projectData()).getData(ProjectExtData.class))
                .properties;
        return Map.of(
                "LAUNCHER_VERSION", "1.0",
                "FORGE_SPEC", subprojectProperties.get("SPEC_VERSION")
        );
    }


    @Override
    public List<String> getVMArgs(Environment env) {
        List<String> vmArgs = new ArrayList<>(NeoForgeFrameworkType.super.getVMArgs(env));
        // todo getSysProps
        vmArgs.add("-Dmixin.env.disableRefMap=true");
        var ignores = List.of(
                "bootstraplauncher",
                "asm-tree",
                "asm",
                "asm-commons",
                "asm-util",
                "asm-analysis",
                "securejarhandler",
                "JarJarFileSystems",
                "events-",
                "language-minecraft",
                "language-java",
                "language-lowcode",
                "core-",
                "client-extra",
                "neoforge-",
                "forge-"
        );
        vmArgs.add("-Dignorelist=" + String.join(",", ignores));
        return vmArgs;
    }

    @Override
    public List<String> buildCliProperties(Module rootModule, Module module) {
        var rootGradleData = requireNonNull(rootModule.projectData());
        var rootProperties = requireNonNull(rootGradleData.getData(ProjectExtData.class))
                .properties;
        return List.of(
                "--version", rootGradleData.version,
                "--fml.mcVersion", rootProperties.get("MC_VERSION"),
                "--fml.forgeVersion", rootProperties.get("FORGE_VERSION"),
                "--fml.fmlVersion", rootProperties.get("FORGE_VERSION"),
                "--fml.mcpVersion", rootProperties.get("MCP_VERSION")
        );
    }

    public @Nullable SRGRemapper getLegacyMappings(Module nfSubModule) {
        var data = nfSubModule.projectData();
        if (data == null) return null;

        var configurations = data.getData(ConfigurationList.class);
        if (configurations == null) return null;

        var runtimeClasspath = configurations.get("runtimeClasspath");
        if (runtimeClasspath == null) return null;

        return FastStream.of(runtimeClasspath.dependencies)
                .mapNonNull(e -> e instanceof ConfigurationData.MavenDependency m ? m : null)
                .filter(e -> e.mavenNotation.group.equals("net.minecraft") && e.mavenNotation.module.equals("mappings_official"))
                .mapNonNull(e -> e.files.get("classes"))
                .map(File::toPath)
                .map(SRGRemapper::fromMcpZip)
                .firstOrDefault();
    }
}
