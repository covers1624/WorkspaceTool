package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.gradle.api.ExtractTestBase;
import net.covers1624.wstool.gradle.api.data.ConfigurationData;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.SubProjectList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by covers1624 on 1/29/25.
 */
public class NeoDevExtractionTests extends ExtractTestBase {

    @Test
    public void test_Neo_1_21_1(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "9e0f669ec00098caaa90c24399c3f43e98c15a6b");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge");
    }

    @Test
    public void test_Neo_1_21_1_regressionSourceSetsNotExtractedBeforeConfigurations(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "f7a5bc85bff4ba5d5a2fd5e521eaa375d52dbadf");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge");
    }

    @Test
    public void test_Neo_1_21_1_LWJGLDependenciesNotBeingExtractedCorrectly(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "f7a5bc85bff4ba5d5a2fd5e521eaa375d52dbadf");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge");

        assertThat(data.getData(SubProjectList.class))
                .isNotNull()
                .extracting(e -> e.get("base"))
                .isNotNull()
                .extracting(e -> e.getData(ConfigurationList.class))
                .isNotNull()
                .extracting(e -> e.get("compileClasspath"))
                .isNotNull()
                .extracting(e -> FastStream.of(e.dependencies)
                        .filter(d ->
                                d instanceof ConfigurationData.MavenDependency dep
                                && dep.mavenNotation.group.equals("org.lwjgl")
                                && dep.mavenNotation.module.equals("lwjgl")
                        )
                        .map(d -> ((ConfigurationData.MavenDependency) d))
                        .onlyOrDefault()
                )
                .isNotNull()
                .extracting(e -> e.children.size())
                .isEqualTo(2);
    }

    @Test
    public void test_Neo_1_21_1_regressionProjectDependenciesMissing(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "f7a5bc85bff4ba5d5a2fd5e521eaa375d52dbadf");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge");

        assertThat(data.getData(SubProjectList.class))
                .isNotNull()
                .extracting(e -> e.get("tests"))
                .isNotNull()
                .extracting(e -> e.getData(ConfigurationList.class))
                .isNotNull()
                .extracting(e -> e.get("compileClasspath"))
                .isNotNull()
                .extracting(e -> e.dependencies.size())
                .isNotEqualTo(1);
    }

    @Test
    public void test_Neo_1_21_4(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.x", "b19a079c7556083c6b77a191a7bbb13898a94972");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge-Root");
    }
}
