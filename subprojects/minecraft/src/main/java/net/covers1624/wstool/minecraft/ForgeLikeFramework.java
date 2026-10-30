package net.covers1624.wstool.minecraft;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.SourceSet;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by covers1624 on 7/4/25.
 */
public interface ForgeLikeFramework extends FrameworkType {

    default List<String> getModIds(SourceSet ss) {
        var ids = FastStream.of(ss.sourcePaths().getOrDefault("resources", List.of()))
                .flatMap(ForgeLikeFramework::parseModsToml)
                .toList();
        if (!ids.isEmpty()) {
            return ids;
        }
        return List.of(ss.module().name());
    }

    private static List<String> parseModsToml(Path folder) {
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
                    .toList();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read mods.toml file.");
        }
    }
}
