package net.covers1624.wstool.minecraft;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.Workspace;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 7/4/25.
 */
public interface ForgeLikeFramework extends FrameworkType {

    // TODO extract this information from Gradle.
    default List<Path> collectAccessTransformers(Workspace workspace) {
        return workspace.allProjectModules()
                .flatMap(e -> e.sourceSets().values())
                .flatMap(e -> e.sourcePaths().get("resources"))
                .filter(Files::exists)
                .flatMap(e -> {
                    try (Stream<Path> files = Files.walk(e)) {
                        return FastStream.of(files)
                                .filter(f -> f.getFileName().toString().equals("accesstransformer.cfg"))
                                .toList();
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to walk files.", ex);
                    }
                })
                .sorted()
                .toList();
    }

    // TODO extract this information from Gradle.
    default List<Path> collectInterfaceInjections(Workspace workspace) {
        return workspace.allProjectModules()
                .flatMap(e -> e.sourceSets().values())
                .flatMap(e -> e.sourcePaths().get("resources"))
                .filter(Files::exists)
                .flatMap(e -> {
                    try (Stream<Path> files = Files.walk(e)) {
                        return FastStream.of(files)
                                .filter(f -> {
                                    var fName = f.getFileName().toString();
                                    return fName.equals("interfaces.json") || fName.equals("interface-injections.json");
                                })
                                .toList();
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to walk files.", ex);
                    }
                })
                .sorted()
                .toList();
    }

    default List<ModSourceSet> findPossibleMods(Workspace workspace) {
        return workspace.allProjectModules()
                .flatMap(e -> e.sourceSets().values())
                .flatMap(e -> FastStream.of(e.sourcePaths().getOrDefault("resources", List.of())).flatMap(f -> parseMods(e, f)))
                .sorted()
                .toList(FastStream.infer());
    }

    private static List<ModSourceSet> parseMods(SourceSet sourceSet, Path folder) {
        Path modsToml = folder.resolve("META-INF/mods.toml");
        if (Files.notExists(modsToml)) {
            modsToml = folder.resolve("META-INF/neoforge.mods.toml");
        }
        if (Files.notExists(modsToml)) return List.of();

        try {
            var toml = Toml.parse(modsToml);
            return FastStream.of(toml.getArray("mods").toList())
                    .map(e -> ((TomlTable) e))
                    .map(e -> e.getString("modId"))
                    .map(e -> new ModSourceSet(e, sourceSet))
                    .toList();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read mods.toml file.");
        }
    }

    record ModSourceSet(String modId, SourceSet ss) { }
}
