package net.covers1624.wstool.gradle.extract;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.GradleTestBase;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.MavenDependency;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.ProjectDependency;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.SourceSetDependency;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ConfigurationExtractionTests extends GradleTestBase {

    @ValueSource (strings = {
            "4.10.3",
            "7.3",
            "8.0",
            "9.0.0",
    })
    @ParameterizedTest
    public void testSimpleDependency(String gradleVersion) throws IOException {
        GradleEmitter emitter = gradleEmitter("SimpleDependency")
                .rootProject()
                // language=Groovy
                .withBuildGradle("""
                        plugins {
                            id 'java'
                        }
                        repositories {
                            mavenCentral()
                        }
                        dependencies {
                            implementation 'com.google.guava:guava:31.0.1-jre'
                        }
                        """)
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );
        ConfigurationList configurations = data.getData(ConfigurationList.class);
        assertNotNull(configurations);

        ConfigurationData compileClasspath = configurations.get("compileClasspath");
        assertThat(compileClasspath)
                .isNotNull();

        var guava = assertThat(compileClasspath.dependencies)
                .hasSize(1)
                .first()
                .isInstanceOf(MavenDependency.class)
                .extracting(e -> (MavenDependency) e);
        guava.extracting(e -> e.mavenNotation)
                .isEqualTo(MavenNotation.parse("com.google.guava:guava:31.0.1-jre"));
        guava.extracting(e -> e.children.size())
                .isEqualTo(6);
        guava.extracting(e -> e.files.get("classes")).isNotNull();
        guava.extracting(e -> e.files.get("sources")).isNotNull();
        guava.extracting(e -> e.files.get("javadoc")).isNotNull();
    }

    @ValueSource (strings = {
            "4.10.3",
            "7.3",
            "8.0",
            "9.0.0",
    })
    @ParameterizedTest
    public void testSourceSetDependency(String gradleVersion) throws IOException {
        GradleEmitter emitter = gradleEmitter("ProjectDependency")
                .rootProject()
                // language=Groovy
                .withBuildGradle("""
                        plugins {
                            id 'java'
                        }
                        sourceSets {
                            core
                        }
                        dependencies {
                            implementation sourceSets.core.output
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );
        SourceSetList sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        ConfigurationList configurations = data.getData(ConfigurationList.class);
        assertNotNull(configurations);

        SourceSetData core = sourceSets.get("core");
        assertNotNull(core);

        ConfigurationData compileClasspath = configurations.get("compileClasspath");
        assertNotNull(compileClasspath);

        assertEquals(1, compileClasspath.dependencies.size());
        assertThat(compileClasspath.dependencies)
                .first()
                .isInstanceOf(SourceSetDependency.class)
                .extracting(e -> ((SourceSetDependency) e).sourceSet)
                .isEqualTo(core);
    }

    @ValueSource (strings = {
            "4.10.3",
            "7.3",
            "8.0",
            "9.0.0",
    })
    @ParameterizedTest
    public void testProjectDependency(String gradleVersion) throws IOException {
        GradleEmitter emitter = gradleEmitter("ProjectDependency")
                .rootProject()
                .withBuildGradle("")
                // language=Groovy
                .withSettingsGradle("""
                        include 'project_a'
                        include 'project_b'
                        """)
                // language=Groovy
                .withFile("project_a/build.gradle", """
                        plugins {
                            id 'java'
                        }
                        """
                )
                // language=Groovy
                .withFile("project_b/build.gradle", """
                        plugins {
                            id 'java'
                        }
                        dependencies {
                            implementation project(':project_a')
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version(gradleVersion),
                Set.of()
        );
        SubProjectList subProjects = data.getData(SubProjectList.class);
        assertNotNull(subProjects);

        assertEquals(2, subProjects.asMap().size());

        ProjectData projectA = subProjects.get("project_a");
        ProjectData projectB = subProjects.get("project_b");
        assertNotNull(projectA);
        assertNotNull(projectB);

        ConfigurationList configurations = projectB.getData(ConfigurationList.class);
        assertNotNull(configurations);

        ConfigurationData compileClasspath = configurations.get("compileClasspath");
        assertNotNull(compileClasspath);

        assertThat(compileClasspath.dependencies)
                .hasSize(1)
                .first()
                .isInstanceOf(ProjectDependency.class)
                .extracting(e -> ((ProjectDependency) e).project)
                .isEqualTo(projectA);
    }
}
