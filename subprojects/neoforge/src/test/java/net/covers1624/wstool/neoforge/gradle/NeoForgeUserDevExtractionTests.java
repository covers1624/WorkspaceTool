package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.GradleEmitter;
import net.covers1624.wstool.gradle.api.GradleTestBase;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetAttribute;
import net.covers1624.wstool.gradle.api.data.SourceSetList;
import net.covers1624.wstool.minecraft.AccessTransformerData;
import net.covers1624.wstool.minecraft.InterfaceInjectionData;
import net.covers1624.wstool.minecraft.ModLoaderAttribute;
import net.covers1624.wstool.minecraft.SideAttribute;
import net.covers1624.wstool.neoforge.gradle.api.NeoForgeGradleVersion;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 1/11/25.
 */
public class NeoForgeUserDevExtractionTests extends GradleTestBase {

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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.gradle' version '[6.0,6.2)'
                        }
                        
                        minecraft {
                            mappings channel: 'official', version: '1.20.1'
                            accessTransformer 'src/main/resources/META-INF/accesstransformer.cfg'
                            accessTransformer 'src/main/resources/META-INF/accesstransformer_different.cfg'
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
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE_USER_DEV, version.variant);
        assertTrue(version.version.startsWith("6.0."));

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.NEO_FORGE, SideAttribute.JOINED));

        assertAccessTransformers(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );
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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
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
                        
                        accessTransformers {
                            file 'src/main/resources/META-INF/accesstransformer.cfg'
                            file 'src/main/resources/META-INF/accesstransformer_different.cfg'
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
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE_USER_DEV, version.variant);
        assertEquals("7.0.97", version.version);

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.NEO_FORGE, SideAttribute.JOINED));

        assertAccessTransformers(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );
    }

    @Test
    public void testNG_7_0_166() throws IOException {
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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
                .withFile("src/main/resources/META-INF/interfaces.json", "{}")
                .withFile("src/main/resources/META-INF/interfaces_different.json", "{}")
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.gradle.userdev' version '7.0.166'
                        }
                        
                        dependencies {
                            implementation "net.neoforged:neoforge:20.4.237"
                        }
                        
                        accessTransformers {
                            file 'src/main/resources/META-INF/accesstransformer.cfg'
                            file 'src/main/resources/META-INF/accesstransformer_different.cfg'
                        }
                        
                        interfaceInjections {
                            file 'src/main/resources/META-INF/interfaces.json'
                            file 'src/main/resources/META-INF/interfaces_different.json'
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("8.10"),
                Set.of()
        );
        NeoForgeGradleVersion version = data.pluginData().getData(NeoForgeGradleVersion.class);
        assertNotNull(version);
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE_USER_DEV, version.variant);
        assertEquals("7.0.166", version.version);

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.NEO_FORGE, SideAttribute.JOINED));

        assertAccessTransformers(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );

        assertInterfaceInjections(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces.json"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces_different.json")
                )
        );
    }

    @Test
    public void testNG_7_0_166Vanilla() throws IOException {
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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
                .withFile("src/main/resources/META-INF/interfaces.json", "{}")
                .withFile("src/main/resources/META-INF/interfaces_different.json", "{}")
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.gradle.neoform' version '7.0.166'
                        }
                        
                        dependencies {
                            implementation "net.minecraft:neoform_client:1.21.1-20240808.144430"
                        }
                        
                        accessTransformers {
                            file 'src/main/resources/META-INF/accesstransformer.cfg'
                            file 'src/main/resources/META-INF/accesstransformer_different.cfg'
                        }
                        
                        interfaceInjections {
                            file 'src/main/resources/META-INF/interfaces.json'
                            file 'src/main/resources/META-INF/interfaces_different.json'
                        }
                        """
                )
                .finish();

        GradleModelExtractor extractor = extractor(emitter, false);
        ProjectData data = extractor.extractProjectData(
                emitter.getRootProjectDir(),
                GradleVersion.version("8.10"),
                Set.of()
        );
        NeoForgeGradleVersion version = data.pluginData().getData(NeoForgeGradleVersion.class);
        assertNotNull(version);
        assertEquals(NeoForgeGradleVersion.Variant.NEO_GRADLE_NEO_FORM, version.variant);
        assertEquals("7.0.166", version.version);

        ConfigurationList configurationData = data.getData(ConfigurationList.class);
        assertNotNull(configurationData);
        assertNoDependencies(configurationData, "compileClasspath");
        assertNoDependencies(configurationData, "runtimeClasspath");

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.VANILLA, SideAttribute.CLIENT));

        assertAccessTransformers(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );

        assertInterfaceInjections(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces.json"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces_different.json")
                )
        );
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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
                .withFile("src/main/resources/META-INF/interfaces.json", "{}")
                .withFile("src/main/resources/META-INF/interfaces_different.json", "{}")
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.moddev' version '2.0.76'
                        }
                        
                        neoForge {
                            version '21.1.72'
                        
                            accessTransformers {
                                publish file('src/main/resources/META-INF/accesstransformer.cfg')
                                from('src/main/resources/META-INF/accesstransformer_different.cfg')
                            }
                        
                            interfaceInjectionData {
                                from('src/main/resources/META-INF/interfaces.json')
                                from('src/main/resources/META-INF/interfaces_different.json')
                            }
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

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.NEO_FORGE, SideAttribute.JOINED));

        assertAccessTransformers(
                data,
                List.of(
                    emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                    emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );

        assertInterfaceInjections(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces.json"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces_different.json")
                )
        );
    }

    @Test
    public void testMDG_2_0Vanilla() throws IOException {
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
                .withFile("src/main/resources/META-INF/accesstransformer.cfg", "")
                .withFile("src/main/resources/META-INF/accesstransformer_different.cfg", "")
                .withFile("src/main/resources/META-INF/interfaces.json", "{}")
                .withFile("src/main/resources/META-INF/interfaces_different.json", "{}")
                .withBuildGradle("""
                        plugins {
                            id 'eclipse'
                            id 'idea'
                            id 'maven-publish'
                            id 'net.neoforged.moddev' version '2.0.76'
                        }
                        
                        neoForge {
                            neoFormVersion = "1.21.1-20240808.144430"
                        
                            accessTransformers {
                                publish file('src/main/resources/META-INF/accesstransformer.cfg')
                                from('src/main/resources/META-INF/accesstransformer_different.cfg')
                            }
                        
                            interfaceInjectionData {
                                from('src/main/resources/META-INF/interfaces.json')
                                from('src/main/resources/META-INF/interfaces_different.json')
                            }
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

        var sourceSets = data.getData(SourceSetList.class);
        assertNotNull(sourceSets);
        assertAttributes(sourceSets, "main", Set.of(ModLoaderAttribute.VANILLA, SideAttribute.JOINED));

        assertAccessTransformers(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer.cfg"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/accesstransformer_different.cfg")
                )
        );

        assertInterfaceInjections(
                data,
                List.of(
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces.json"),
                        emitter.getRootProjectDir().resolve("src/main/resources/META-INF/interfaces_different.json")
                )
        );
    }

    private void assertNoDependencies(ConfigurationList configurations, String name) {
        var configuration = configurations.get(name);
        assertThat(configuration)
                .isNotNull();
        assertThat(configuration.dependencies)
                .isEmpty();
    }

    private void assertAttributes(SourceSetList sourceSets, String name, Set<SourceSetAttribute> expected) {
        var sourceSet = sourceSets.get(name);
        assertNotNull(sourceSet);
        assertEquals(expected, sourceSet.attributes);
    }

    private void assertAccessTransformers(ProjectData projectData, List<Path> files) {
        var atData = projectData.getData(AccessTransformerData.class);
        assertNotNull(atData);
        assertEquals(FastStream.of(files).map(Path::toFile).toList(), atData.files);
    }

    private void assertInterfaceInjections(ProjectData projectData, List<Path> files) {
        var ifaceData = projectData.getData(InterfaceInjectionData.class);
        assertNotNull(ifaceData);
        assertEquals(FastStream.of(files).map(Path::toFile).toList(), ifaceData.files);
    }
}
