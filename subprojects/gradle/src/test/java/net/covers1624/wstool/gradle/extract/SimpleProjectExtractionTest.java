package net.covers1624.wstool.gradle.extract;

import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.GradleTestBase;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import net.covers1624.wstool.gradle.api.data.SourceSetDataList;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by covers1624 on 1/9/23.
 */
public class SimpleProjectExtractionTest extends GradleTestBase {

    // @formatter:off
    @Test public void testBlank4_10_3() throws Throwable { testBlank("4.10.3"); }
    @Test public void testBlank7_3() throws Throwable { testBlank("7.3"); }
    @Test public void testBlank8_0() throws Throwable { testBlank("8.0"); }
    // @formatter:on

    private ProjectData testBlank(String gradleVersion) throws IOException {
        GradleEmitter emitter = gradleEmitter("BlankProject")
                .rootProject()
                .withBuildGradle("")
                .finish();

        GradleModelExtractor extractor = new GradleModelExtractor(emitter.getTempDir(), emitter.getTempDir(), JDK_PROVIDER);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );

        assertEquals("BlankProject", data.name);
        assertEquals(emitter.getRootProjectDir().toFile(), data.projectDir);

        return data;
    }

    // @formatter:off
    @Test public void testJavaPlugin4_10_3() throws Throwable { testJavaPlugin("4.10.3"); }
    @Test public void testJavaPlugin7_3() throws Throwable { testJavaPlugin("7.3"); }
    @Test public void testJavaPlugin8_0() throws Throwable { testJavaPlugin("8.0"); }
    // @formatter:on

    private ProjectData testJavaPlugin(String gradleVersion) throws IOException {
        GradleEmitter emitter = gradleEmitter("JavaPlugin")
                .rootProject()
                // language=Groovy
                .withBuildGradle("""
                        plugins {
                            id 'java'
                        }
                        """)
                .finish();

        GradleModelExtractor extractor = new GradleModelExtractor(emitter.getTempDir(), emitter.getTempDir(), JDK_PROVIDER);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );

        SourceSetDataList sourceSetList = data.getData(SourceSetDataList.class);
        assertNotNull(sourceSetList);

        SourceSetData main = sourceSetList.sourceSets.get("main");
        assertNotNull(main);
        assertEquals("main", main.name);
        assertNotNull(main.compileClasspathConfiguration);
        assertNotNull(main.runtimeClasspathConfiguration);
        assertNotNull(main.sourceMap.get("java"));
        assertNotNull(main.sourceMap.get("resources"));

        SourceSetData test = sourceSetList.sourceSets.get("test");
        assertNotNull(test);
        assertEquals("test", test.name);
        assertNotNull(test.compileClasspathConfiguration);
        assertNotNull(test.runtimeClasspathConfiguration);
        assertNotNull(test.sourceMap.get("java"));
        assertNotNull(test.sourceMap.get("resources"));

        return data;
    }
}
