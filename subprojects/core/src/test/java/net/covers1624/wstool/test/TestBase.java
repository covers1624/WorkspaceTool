package net.covers1624.wstool.test;

import com.google.common.collect.Sets;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.CopyingFileVisitor;
import net.covers1624.wstool.WorkspaceTool;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.util.DeletingFileVisitor;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
                    .exists();

            if (!includeLibraries) {
                Files.walkFileTree(ideaDir.resolve("libraries"), new DeletingFileVisitor());
            }

            // This makes the files stable, but, incompatible with Intellij :(
            Set<Path> newFiles = walkRelative(ideaDir);
            for (Path newFileRel : newFiles) {
                Path newFile = ideaDir.resolve(newFileRel);
                String fName = newFile.getFileName().toString();
                if (fName.endsWith(".iml") || fName.endsWith(".xml")) {
                    String str = Files.readString(newFile, StandardCharsets.UTF_8);
                    str = str.replaceAll(projectDir.toString(), "\\$PROJECT_DIR\\$");
                    Files.writeString(newFile, str, StandardCharsets.UTF_8);
                }
            }

            if (!IS_UPDATE) {
                try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
                    Set<Path> oldFiles = walkRelative(outputDir);
                    Set<Path> added = Sets.difference(newFiles, oldFiles);
                    Set<Path> removed = Sets.difference(oldFiles, newFiles);
                    Set<Path> common = Sets.intersection(newFiles, oldFiles);

                    softly.assertThat(added)
                            .withFailMessage("Files were added to the workspace. %s", added)
                            .isEmpty();

                    softly.assertThat(removed)
                            .withFailMessage("Files were removed from the workspace. %s", removed)
                            .isEmpty();

                    for (Path path : common) {
                        Path newFile = ideaDir.resolve(path);
                        Path oldFile = outputDir.resolve(path);
                        softly.assertThat(newFile)
                                .hasSameTextualContentAs(oldFile, StandardCharsets.UTF_8);
                    }
                }
            } else {
                if (Files.exists(outputDir)) {
                    Files.walkFileTree(outputDir, new DeletingFileVisitor(outputDir));
                }
                Files.walkFileTree(ideaDir, new CopyingFileVisitor(ideaDir, outputDir));
            }

            Files.walkFileTree(projectDir, new DeletingFileVisitor());
        }

        private static Set<Path> walkRelative(Path dir) throws IOException {
            try (Stream<Path> stream = Files.walk(dir)) {
                return FastStream.of(stream)
                        .filter(Files::isRegularFile)
                        .map(dir::relativize)
                        .toLinkedHashSet();
            }
        }
    }
}
