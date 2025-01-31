package net.covers1624.wstool.gradle.api;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.GradleModelExtractor;

import java.nio.file.Path;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ExtractTestBase extends GradleTestBase {

    protected static GradleModelExtractor extractor(GradleEmitter emitter, boolean jvmAttach) {
        return extractor(emitter.getTempDir(), emitter.getTempDir(), jvmAttach);
    }

    protected static GradleModelExtractor extractor(Path workspaceRoot, Path cacheDir, boolean jvmAttach) {
        return new GradleModelExtractor(workspaceRoot, cacheDir, JDK_PROVIDER) {
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
