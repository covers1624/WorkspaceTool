package net.covers1624.wstool.gradle.extract;

import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.data.*;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by covers1624 on 1/9/23.
 */
public class SimpleProjectExtractionTest extends ExtractTestBase {

    @ValueSource (strings = {
            "4.10.3",
            "7.3",
            "8.0",
    })
    @ParameterizedTest
    public void testBlank(String gradleVersion) throws IOException {
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
    }

    @ValueSource (strings = {
            "4.10.3",
            "7.3",
            "8.0",
    })
    @ParameterizedTest
    public void testJavaPlugin(String gradleVersion) throws IOException {
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

        var configurations = data.getData(ConfigurationList.class);
        assertThat(configurations).isNotNull();

        var mainCompileClasspathConfiguration = configurations.get(main.compileClasspathConfiguration);
        assertThat(mainCompileClasspathConfiguration)
                .isNotNull();
        assertThat(mainCompileClasspathConfiguration.dependencies)
                .isEmpty();

        var mainRuntimeClasspathConfiguration = configurations.get(main.runtimeClasspathConfiguration);
        assertThat(mainRuntimeClasspathConfiguration)
                .isNotNull();
        assertThat(mainRuntimeClasspathConfiguration.dependencies)
                .isEmpty();

        var testCompileClasspathConfiguration = configurations.get(test.compileClasspathConfiguration);
        assertThat(testCompileClasspathConfiguration)
                .isNotNull();
        assertThat(testCompileClasspathConfiguration.dependencies)
                .isNotEmpty()
                .first()
                .isEqualTo(new ConfigurationData.SourceSetDependency(main));
    }

    @ValueSource (strings = {
            // This Gradle version crashes with java extraction issues with some of my local jvms..
            // "6.7",
            "7.3",
            "8.0",
            "8.14",
    })
    @ParameterizedTest
    public void testExtractJavaToolchainVersion(String version) throws IOException {
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
