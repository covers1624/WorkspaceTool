package net.covers1624.wstool.gradle.api;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 1/9/23.
 */
public class GradleTestBase {

    public static final JdkProvider JDK_PROVIDER = new JdkProvider(Environment.of());

    protected static GradleEmitter gradleEmitter(String name) throws IOException {
        Path tempDir = Files.createTempDirectory("wstool-gradle-test-project");
        tempDir.toFile().deleteOnExit();
        return new GradleEmitter(name, tempDir.resolve(name));
    }
}
