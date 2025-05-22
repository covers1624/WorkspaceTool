package net.covers1624.wstool.gradle.extract;

import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.data.JavaToolchainData;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import net.covers1624.wstool.gradle.api.data.SourceSetList;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by covers1624 on 1/9/23.
 */
public class SimpleProjectExtractionTest extends ExtractTestBase {

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

        GradleModelExtractor extractor = extractor(emitter, false);
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

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );

        SourceSetList sourceSetList = data.getData(SourceSetList.class);
        assertNotNull(sourceSetList);

        SourceSetData main = sourceSetList.get("main");
        assertNotNull(main);
        assertEquals("main", main.name);
        assertNotNull(main.compileClasspathConfiguration);
        assertNotNull(main.runtimeClasspathConfiguration);
        assertNotNull(main.sourceMap.get("java"));
        assertNotNull(main.sourceMap.get("resources"));

        SourceSetData test = sourceSetList.get("test");
        assertNotNull(test);
        assertEquals("test", test.name);
        assertNotNull(test.compileClasspathConfiguration);
        assertNotNull(test.runtimeClasspathConfiguration);
        assertNotNull(test.sourceMap.get("java"));
        assertNotNull(test.sourceMap.get("resources"));

        return data;
    }

    // @formatter:off
    // This Gradle version crashes with java extraction issues with some of my local jvms..
//    @Test public void testExtractJavaToolchainVersion6_7() throws Throwable { testExtractJavaToolchainVersion("6.7"); }
    @Test public void testExtractJavaToolchainVersion7_3() throws Throwable { testExtractJavaToolchainVersion("7.3"); }
    @Test public void testExtractJavaToolchainVersion8_0() throws Throwable { testExtractJavaToolchainVersion("8.0"); }
    @Test public void testExtractJavaToolchainVersion8_14() throws Throwable { testExtractJavaToolchainVersion("8.14"); }
    // @formatter:on

    private void testExtractJavaToolchainVersion(String version) throws IOException {
        GradleEmitter emitter = gradleEmitter("JavaPlugin")
                .rootProject()
                // language=Groovy
                .withBuildGradle("""
                        plugins {
                            id 'java'
                        }
                        
                        java {
                            toolchain {
                                languageVersion = JavaLanguageVersion.of(21)
                            }
                        }
                        """)
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(version),
                Set.of()
        );

        assertThat(data.getData(JavaToolchainData.class))
                .isNotNull()
                .extracting(e -> e.langVersion)
                .isEqualTo(21);
    }
}
