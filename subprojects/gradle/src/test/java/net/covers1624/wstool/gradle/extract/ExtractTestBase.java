package net.covers1624.wstool.gradle.extract;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.GradleTestBase;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ExtractTestBase extends GradleTestBase {

    protected static GradleModelExtractor extractor(GradleEmitter emitter, boolean jvmAttach) {
        return new GradleModelExtractor(emitter.getTempDir(), emitter.getTempDir(), JDK_PROVIDER) {
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
