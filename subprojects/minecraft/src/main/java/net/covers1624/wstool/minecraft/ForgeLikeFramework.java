package net.covers1624.wstool.minecraft;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.Workspace;

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
                .toList();
    }
}
