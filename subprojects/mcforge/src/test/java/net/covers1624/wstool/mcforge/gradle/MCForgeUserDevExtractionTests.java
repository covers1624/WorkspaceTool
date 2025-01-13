package net.covers1624.wstool.mcforge.gradle;

import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.mcforge.gradle.api.MCForgeGradleVersion;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 1/11/25.
 */
public class MCForgeUserDevExtractionTests extends ExtractTestBase {

    @Test
    public void testFG_2_3() throws IOException {
        GradleEmitter emitter = gradleEmitter("FG2Tests")
                .rootProject()
                .withFile("gradle.properties", "org.gradle.daemon=false")
                .withBuildGradle("""
                        buildscript {
                            repositories {
                                jcenter()
                                maven { url = "https://files.minecraftforge.net/maven" }
                            }
                            dependencies {
                                classpath 'net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT'
                            }
                        }
                        apply plugin: 'net.minecraftforge.gradle.forge'
                        
                        minecraft {
                            version = "1.12.2-14.23.5.2847"
                            runDir = "run"
                            mappings = "snapshot_20171003"
                        }
                        
                        // repositories {
                        //     maven { url 'https://maven.covers1624.net/' }
                        // }
                        //
                        // dependencies {
                        //     deobfCompile 'codechicken:CodeChickenLib:1.12.2-3.2.3.358:universal'
                        // }
                        """)
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("4.10.3"),
                Set.of()
        );
        MCForgeGradleVersion version = data.pluginData().getData(MCForgeGradleVersion.class);
        assertNotNull(version);
        assertTrue(version.version.startsWith("2.3."));
    }

    @Test
    public void testFG_3_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("FG3Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withBuildGradle("""
                        buildscript {
                            repositories {
                                maven { url = 'https://files.minecraftforge.net/maven' }
                                mavenCentral()
                            }
                            dependencies {
                                classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '3.+', changing: true
                            }
                        }
                        apply plugin: 'net.minecraftforge.gradle'
                        minecraft {
                            mappings channel: 'snapshot', version: '20190719-1.14.3'
                        }
                        
                        dependencies {
                            minecraft "net.minecraftforge:forge:1.14.4-28.0.30"
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("4.10.3"),
                Set.of()
        );
        MCForgeGradleVersion version = data.pluginData().getData(MCForgeGradleVersion.class);
        assertNotNull(version);
        assertTrue(version.version.startsWith("3.0."));

        // we should strip out the minecraft configuration from ever being touched.
        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNull(configurationData.get("minecraft"));
    }

    @Test
    public void testFG_4_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("FG4Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withBuildGradle("""
                        buildscript {
                            repositories {
                                maven { url = 'https://files.minecraftforge.net/maven' }
                                mavenCentral()
                            }
                            dependencies {
                                classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '4.1.+', changing: true
                            }
                        }
                        apply plugin: 'net.minecraftforge.gradle'
                        minecraft {
                            mappings channel: 'official', version: '1.16.5'
                        }
                        
                        dependencies {
                            minecraft "net.minecraftforge:forge:1.16.5-36.1.35"
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("6.8.1"),
                Set.of()
        );
        MCForgeGradleVersion version = data.pluginData().getData(MCForgeGradleVersion.class);
        assertNotNull(version);
        assertTrue(version.version.startsWith("4.1."));

        // we should strip out the minecraft configuration from ever being touched.
        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNull(configurationData.get("minecraft"));
    }

    @Test
    public void testFG_5_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("FG4Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withBuildGradle("""
                        buildscript {
                            repositories {
                                maven { url = 'https://files.minecraftforge.net/maven' }
                                mavenCentral()
                            }
                            dependencies {
                                classpath group: 'net.minecraftforge.gradle', name: 'ForgeGradle', version: '5.0.+', changing: true
                            }
                        }
                        apply plugin: 'net.minecraftforge.gradle'
                        minecraft {
                            mappings channel: 'official', version: '1.16.5'
                        }
                        
                        dependencies {
                            minecraft "net.minecraftforge:forge:1.16.5-36.1.35"
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("7.1"),
                Set.of()
        );
        MCForgeGradleVersion version = data.pluginData().getData(MCForgeGradleVersion.class);
        assertNotNull(version);
        assertTrue(version.version.startsWith("5.0."));

        // we should strip out the minecraft configuration from ever being touched.
        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNull(configurationData.get("minecraft"));
    }

    @Test
    public void testFG_6_0() throws IOException {
        GradleEmitter emitter = gradleEmitter("FG6Tests")
                .rootProject()
                .withGradleProperties("org.gradle.daemon=false")
                .withSettingsGradle("""
                        pluginManagement {
                            repositories {
                                gradlePluginPortal()
                                maven { url = 'https://maven.minecraftforge.net/' }
                                maven {
                                    name = 'MinecraftForge'
                                    url = 'https://maven.minecraftforge.net/'
                                }
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
                            id 'net.minecraftforge.gradle' version '[6.0,6.2)'
                        }
                        
                        minecraft {
                            mappings channel: 'official', version: '1.16.5'
                        }
                        
                        dependencies {
                            minecraft "net.minecraftforge:forge:1.16.5-36.2.42"
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
        MCForgeGradleVersion version = data.pluginData().getData(MCForgeGradleVersion.class);
        assertNotNull(version);
        assertTrue(version.version.startsWith("6.0."));

        // we should strip out the minecraft configuration from ever being touched.
        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNull(configurationData.get("minecraft"));
    }
}
