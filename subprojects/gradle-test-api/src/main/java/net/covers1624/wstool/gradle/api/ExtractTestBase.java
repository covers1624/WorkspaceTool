package net.covers1624.wstool.gradle.api;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.gradle.GradleModelExtractor;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ExtractTestBase extends GradleTestBase {

    protected static Environment testEnvironment(Path projectDir) {
        return Environment.of(null, projectDir.resolve(".wstool_sys/"), projectDir);
    }

    protected static GradleModelExtractor extractor(GradleEmitter emitter, boolean jvmAttach) {
        return extractor(testEnvironment(emitter.getTempDir()), jvmAttach);
    }

    protected static GradleModelExtractor extractor(Environment env, boolean jvmAttach) {
        return new GradleModelExtractor(env, JDK_PROVIDER, List.of()) {
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
