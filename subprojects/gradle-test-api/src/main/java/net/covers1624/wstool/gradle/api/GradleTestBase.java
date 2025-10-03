package net.covers1624.wstool.gradle.api;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.okhttp.OkHttpEngine;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.gradle.GradleModelExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by covers1624 on 1/9/23.
 */
public class GradleTestBase {

    private static final HttpEngine HTTP_ENGINE = OkHttpEngine.create();
    private static final Environment REAL_ENV = Environment.of();
    static {
        REAL_ENV.putService(HttpEngine.class, HTTP_ENGINE);
    }

    private static final JdkProvider JDK_PROVIDER = new JdkProvider(REAL_ENV);

    protected static GradleEmitter gradleEmitter(String name) throws IOException {
        Path tempDir = Files.createTempDirectory("wstool-gradle-test-project");
        tempDir.toFile().deleteOnExit();
        return new GradleEmitter(name, tempDir.resolve(name));
    }

    protected static Environment testEnvironment(Path projectDir) {
        var env = Environment.of(projectDir.resolve(".wstool_sys/"), projectDir);
        env.putService(HttpEngine.class, HTTP_ENGINE);
        env.putService(JdkProvider.class, JDK_PROVIDER);
        return env;
    }

    protected static GradleModelExtractor extractor(GradleEmitter emitter, boolean jvmAttach) {
        return extractor(testEnvironment(emitter.getRootProjectDir().getParent()), jvmAttach);
    }

    protected static GradleModelExtractor extractor(Environment env, boolean jvmAttach) {
        return new GradleModelExtractor(env, List.of()) {
            @Override
            protected Iterable<String> extraJvmArgs() {
                if (jvmAttach) {
                    return FastStream.of("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5555");
                }
                return super.extraJvmArgs();
            }
        };
    }
}
