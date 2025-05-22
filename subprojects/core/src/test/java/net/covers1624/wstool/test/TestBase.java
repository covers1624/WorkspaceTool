package net.covers1624.wstool.test;

import net.covers1624.quack.io.CopyingFileVisitor;
import net.covers1624.wstool.WorkspaceTool;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.util.DeletingFileVisitor;
import org.assertj.core.api.Condition;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Created by covers1624 on 5/22/25.
 */
public abstract class TestBase {

    private static final boolean IS_UPDATE = Boolean.parseBoolean(System.getProperty("net.covers1624.covers1624.wstool.test_update", "true"));

    protected TestInstance newTestInstance(String name) throws IOException {
        return new TestInstance(name);
    }

    public static class TestInstance implements AutoCloseable {

        private final Path outputDir;
        private final Path projectDir;

        private boolean includeLibraries = false;

        public TestInstance(String name) throws IOException {
            outputDir = Path.of("./testOutput/" + name);
            projectDir = Files.createTempDirectory("test").resolve(name);
        }

        public TestInstance includeLibraries() {
            includeLibraries = true;
            return this;
        }

        public TestInstance cloneRepo(String path, String repo, String branch, String commit) throws IOException {
            GitRepoManager repoManager = new GitRepoManager(projectDir.resolve(path));
            repoManager.setConfig(repo, branch, commit);
            repoManager.checkout();
            return this;
        }

        public TestInstance gradleProject(String path, String name, Consumer<GradleEmitter> cons) {
            GradleEmitter emitter = new GradleEmitter(name, projectDir.resolve(path));
            cons.accept(emitter);
            return this;
        }

        public TestInstance emitWorkspaceFile(@Language ("yaml") String file) throws IOException {
            Files.writeString(projectDir.resolve("workspace.yml"), file);
            return this;
        }

        public void run() throws IOException {
            Environment env = Environment.of(
                    null,
                    projectDir.resolve(".wstool_sys"),
                    projectDir
            );
            WorkspaceTool.run(env);
        }

        @Override
        public void close() throws IOException {
            Path ideaDir = projectDir.resolve(".idea");
            assertThat(ideaDir)
                    .is(fileExists(ideaDir));

            if (!IS_UPDATE) {
                List<Path> newFiles = listDir(ideaDir);
                for (Path newFile : newFiles) {
                    Path rel = ideaDir.relativize(newFile);
                    Path old = outputDir.resolve(rel);

                    assertThat(old).is(fileExists("File was removed. %s", old));
                    assertThat(Files.readString(newFile)).isEqualTo(Files.readString(old));
                }

                List<Path> oldFiles = listDir(outputDir);
                for (Path oldFile : oldFiles) {
                    Path rel = ideaDir.relativize(oldFile);
                    Path newFile = outputDir.resolve(rel);

                    assertThat(newFile).is(fileExists("File was added. %s", newFile));
                    assertThat(Files.readString(oldFile)).isEqualTo(Files.readString(newFile));
                }
            } else {
                if (Files.exists(outputDir)) {
                    Files.walkFileTree(outputDir, new DeletingFileVisitor(outputDir));
                }
                Files.walkFileTree(ideaDir, new CopyingFileVisitor(ideaDir, outputDir, e -> includeLibraries || !e.toString().startsWith("libraries/")));
            }

            Files.walkFileTree(projectDir, new DeletingFileVisitor());
        }

        public static Condition<Path> fileExists() {
            return fileExists("Expected file to exist.");
        }

        public static Condition<Path> fileExists(Path file) {
            return fileExists("Expected file %s to exist.", file);
        }

        public static Condition<Path> fileExists(String desc, Object... args) {
            return new Condition<>(Files::exists, desc, args);
        }

        private static List<Path> listDir(Path dir) throws IOException {
            try (Stream<Path> stream = Files.list(dir)) {
                return stream.toList();
            }
        }
    }
}
