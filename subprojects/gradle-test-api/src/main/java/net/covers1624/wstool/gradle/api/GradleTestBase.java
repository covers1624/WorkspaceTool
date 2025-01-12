package net.covers1624.wstool.gradle.api;

import net.covers1624.quack.io.IOUtils;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.api.WorkspaceToolEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 1/9/23.
 */
public class GradleTestBase {

    public static final JdkProvider JDK_PROVIDER = new JdkProvider(WorkspaceToolEnvironment.WSTOOL_JDKS);

    protected static GradleEmitter gradleEmitter(String name) throws IOException {
        return new GradleEmitter(name);
    }

    public static class GradleEmitter {

        private final String projectName;
        private final Path tempDir;
        private final Path rootDir;

        private GradleEmitter(String projectName) throws IOException {
            this.projectName = projectName;
            tempDir = Files.createTempDirectory("wstool-gradle-test-project");
            tempDir.toFile().deleteOnExit();

            rootDir = tempDir.resolve(projectName);
        }

        public Path getTempDir() {
            return tempDir;
        }

        public Path getRootProjectDir() {
            return rootDir;
        }

        public ProjectEmitter rootProject() {
            return new ProjectEmitter(projectName, rootDir);
        }

        public ProjectEmitter subProject(String name) {
            return subProject(name, name);
        }

        public ProjectEmitter subProject(String name, String path) {
            return new ProjectEmitter(name, rootDir.resolve(path));
        }

        public class ProjectEmitter {

            public final String name;
            public final Path dir;

            public ProjectEmitter(String name, Path dir) {
                this.name = name;
                this.dir = dir;
            }

            public ProjectEmitter withBuildGradle(String buildGradle) throws IOException {
                return withFile("build.gradle", buildGradle);
            }

            public ProjectEmitter withSettingsGradle(String buildGradle) throws IOException {
                return withFile("settings.gradle", buildGradle);
            }

            public ProjectEmitter withGradleProperties(String properties) throws IOException  {
                return withFile("gradle.properties", properties);
            }

            public ProjectEmitter withFile(String path, String content) throws IOException {
                Files.writeString(IOUtils.makeParents(dir.resolve(path)), content);
                return this;
            }

            public ProjectEmitter withFile(String path, byte[] content) throws IOException {
                Files.write(IOUtils.makeParents(dir.resolve(path)), content);
                return this;
            }

            public Path getDir() {
                return dir;
            }

            public GradleEmitter finish() {
                return GradleEmitter.this;
            }
        }
    }
}
