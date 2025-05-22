package net.covers1624.wstool.gradle.api;

import net.covers1624.quack.io.IOUtils;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 5/22/25.
 */
public class GradleEmitter {

    private final String projectName;
    private final Path rootDir;

    public GradleEmitter(String projectName, Path rootDir) {
        this.projectName = projectName;
        this.rootDir = rootDir;
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

        public ProjectEmitter withBuildGradle(@Language ("gradle") String buildGradle) throws IOException {
            return withFile("build.gradle", buildGradle);
        }

        public ProjectEmitter withSettingsGradle(@Language ("gradle") String buildGradle) throws IOException {
            return withFile("settings.gradle", buildGradle);
        }

        public ProjectEmitter withGradleProperties(@Language ("properties") String properties) throws IOException {
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
