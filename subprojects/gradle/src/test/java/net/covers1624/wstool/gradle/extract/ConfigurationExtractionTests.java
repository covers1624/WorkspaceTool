package net.covers1624.wstool.gradle.extract;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.MavenDependency;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.ProjectDependency;
import net.covers1624.wstool.gradle.api.data.ConfigurationData.SourceSetDependency;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 9/9/23.
 */
public class ConfigurationExtractionTests extends ExtractTestBase {

    // @formatter:off
    @Test public void testSimpleDependency_4_10_3() throws Throwable { testSimpleDependency("4.10.3"); }
    @Test public void testSimpleDependency_7_3() throws Throwable { testSimpleDependency("7.3"); }
    @Test public void testSimpleDependency_8_0() throws Throwable { testSimpleDependency("8.0"); }
    // @formatter:on

    private void testSimpleDependency(String gradleVersion) throws IOException {
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
        assertNotNull(compileClasspath);

        assertEquals(1, compileClasspath.dependencies.size());
        MavenDependency guava = (MavenDependency) compileClasspath.dependencies.get(0);

        assertEquals(MavenNotation.parse("com.google.guava:guava:31.0.1-jre"), guava.mavenNotation);
        assertEquals(6, guava.children.size());
        assertNotNull(guava.files.get("classes"));
        assertNotNull(guava.files.get("sources"));
        assertNotNull(guava.files.get("javadoc"));
    }

    // @formatter:off
    @Test public void testSourceSetDependency_4_10_3() throws Throwable { testSourceSetDependency("4.10.3"); }
    @Test public void testSourceSetDependency_7_3() throws Throwable { testSourceSetDependency("7.3"); }
    @Test public void testSourceSetDependency_8_0() throws Throwable { testSourceSetDependency("8.0"); }
    // @formatter:on

    private void testSourceSetDependency(String gradleVersion) throws IOException {
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
        SourceSetDependency dep = (SourceSetDependency) compileClasspath.dependencies.get(0);

        assertEquals(core, dep.sourceSet);
    }

    // @formatter:off
    @Test public void testProjectDependency_4_10_3() throws Throwable { testProjectDependency("4.10.3"); }
    @Test public void testProjectDependency_7_3() throws Throwable { testProjectDependency("7.3"); }
    @Test public void testProjectDependency_8_0() throws Throwable { testProjectDependency("8.0"); }
    // @formatter:on

    private void testProjectDependency(String gradleVersion) throws IOException {
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

        assertEquals(1, compileClasspath.dependencies.size());
        ConfigurationData.Dependency dep = compileClasspath.dependencies.get(0);
        assertTrue(dep instanceof ProjectDependency);

        ProjectDependency projectDependency = (ProjectDependency) dep;
        assertEquals(projectA, projectDependency.project);
    }
}
