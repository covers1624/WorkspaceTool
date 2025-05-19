package net.covers1624.wstool.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by covers1624 on 1/31/25.
 */
public class ModuleUtilsTests {

    @Test
    public void testSingle(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("asdf"));
        assertThat(ModuleUtils.expandModuleReference(dir, "asdf"))
                .hasSize(1)
                .allMatch(Files::exists);
        assertThat(ModuleUtils.expandModuleReference(dir, "asdf/"))
                .hasSize(1)
                .allMatch(Files::exists);

        assertThatThrownBy(() -> ModuleUtils.expandModuleReference(dir, "/asdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Module reference must not start with a slash. /asdf");
    }

    @Test
    public void testFolderGlob(@TempDir Path dir) throws IOException {
        var expectedFolders = List.of(
                dir.resolve("asdf/a"),
                dir.resolve("asdf/b"),
                dir.resolve("asdf/c"),
                dir.resolve("asdf/d")
        );
        for (Path e : expectedFolders) {
            Files.createDirectories(e);
        }

        assertThat(ModuleUtils.expandModuleReference(dir, "asdf/**"))
                .containsExactlyInAnyOrderElementsOf(expectedFolders);

        assertThat(ModuleUtils.expandModuleReference(dir, "fdsa/**"))
                .isEmpty();
    }

    @Test
    public void testPartialGlob(@TempDir Path dir) throws IOException {
        var expectedFolders = List.of(
                dir.resolve("asdf/aa"),
                dir.resolve("asdf/ab")
        );
        for (Path e : expectedFolders) {
            Files.createDirectories(e);
        }
        Files.createDirectories(dir.resolve("asdf/bb"));
        Files.createDirectories(dir.resolve("asdf/bc"));

        assertThat(ModuleUtils.expandModuleReference(dir, "asdf/a**"))
                .containsExactlyInAnyOrderElementsOf(expectedFolders);

        assertThat(ModuleUtils.expandModuleReference(dir, "fdsa/a**"))
                .isEmpty();
    }
}
