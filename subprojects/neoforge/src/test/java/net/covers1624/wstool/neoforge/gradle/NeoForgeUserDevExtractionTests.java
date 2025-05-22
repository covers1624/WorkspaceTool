package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 1/11/25.
 */
public class NeoForgeUserDevExtractionTests extends ExtractTestBase {

    @Test
    public void testNG_6_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("NG6Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withSettingsGradle("""
                        pluginManagement {
                            repositories {
                                gradlePluginPortal()
                                maven { url 'https://maven.neoforged.net/releases' }
                            }
                        }
                        
                        plugins {
                            id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
                        }
                        """
                )
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.gradle' version '[6.0,6.2)'
                        }
                        
                        minecraft {
                            mappings channel: 'official', version: '1.20.1'
                        }
                        
                        dependencies {
                            minecraft "net.neoforged:forge:1.20.1-47.1.65"
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("8.4"),
                Set.of()
        );
        NeoForgeGradleVersion version = data.pluginData().getData(NeoForgeGradleVersion.class);
        assertNotNull(version);
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE, version.variant);
        assertTrue(version.version.startsWith("6.0."));

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");
    }

    @Test
    public void testNG_7_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("NG7Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withSettingsGradle("""
                        pluginManagement {
                            repositories {
                                gradlePluginPortal()
                                maven { url 'https://maven.neoforged.net/releases' }
                            }
                        }
                        
                        plugins {
                            id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
                        }
                        """
                )
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.gradle.userdev' version '7.0.97'
                        }
                        
                        dependencies {
                            implementation "net.neoforged:neoforge:20.4.237"
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("8.6"),
                Set.of()
        );
        NeoForgeGradleVersion version = data.pluginData().getData(NeoForgeGradleVersion.class);
        assertNotNull(version);
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE, version.variant);
        assertEquals("7.0.97", version.version);

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");
    }

    @Test
    public void testMDG_2_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("MDG2Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withSettingsGradle("""
                        pluginManagement {
                            repositories {
                                gradlePluginPortal()
                                maven { url 'https://maven.neoforged.net/releases' }
                            }
                        }
                        
                        plugins {
                            id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
                        }
                        """
                )
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.moddev' version '2.0.76'
                        }
                        
                        neoForge {
                            version '21.1.72'
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("8.8"),
                Set.of()
        );
        NeoForgeGradleVersion version = data.pluginData().getData(NeoForgeGradleVersion.class);
        assertNotNull(version);
        assertEquals(NeoForgeGradleVersion.Variant.MOD_DEV_GRADLE, version.variant);
        assertEquals("2.0.76", version.version);

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");
    }

    private void assertNoDependencies(ConfigurationList configurations, String name) {
        var configuration = configurations.get(name);
        assertThat(configuration)
                .isNotNull();
        assertThat(configuration.dependencies)
                .isEmpty();
    }
}
