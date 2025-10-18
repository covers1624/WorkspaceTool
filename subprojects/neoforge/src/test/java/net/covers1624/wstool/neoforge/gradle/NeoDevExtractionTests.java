package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.GitRepoManager;
import net.covers1624.wstool.gradle.api.GradleTestBase;
import net.covers1624.wstool.gradle.api.data.ConfigurationData;
import net.covers1624.wstool.gradle.api.data.ConfigurationList;
import net.covers1624.wstool.gradle.api.data.PluginData;
import net.covers1624.wstool.gradle.api.data.SubProjectList;
import net.covers1624.wstool.neoforge.gradle.api.NeoDevData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by covers1624 on 1/29/25.
 */
public class NeoDevExtractionTests extends GradleTestBase {

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
    public void test_Neo_1_21_1_neoDevData(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "f7a5bc85bff4ba5d5a2fd5e521eaa375d52dbadf");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge");
        var subProjectList = data.getData(SubProjectList.class);
        assertThat(subProjectList)
                .isNotNull();

        var nfSubProject = subProjectList.get("neoforge");
        assertThat(nfSubProject)
                .isNotNull();
        var nfSubProjectGradleData = nfSubProject.getData(PluginData.class);
        assertThat(nfSubProjectGradleData)
                .isNotNull();

        var neoDevData = nfSubProjectGradleData.getData(NeoDevData.class);
        assertThat(neoDevData)
                .isNotNull();

        assertThat(nfSubProject)
                .extracting(e -> e.getData(ConfigurationList.class))
                .isNotNull()
                .extracting(e -> e.get(neoDevData.moduleClasspathConfiguration))
                .isNotNull()
                .extracting(e -> e.dependencies.size())
                .isNotEqualTo(0);
    }

    @Test
    public void test_Neo_1_21_1_BuildSrcPlugin(@TempDir Path tempDir) throws Throwable {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("NeoForge"));
        repoManager.setConfig("https://github.com/neoforged/NeoForge.git", "1.21.1", "a9a8f46c05f70bdf85f5ba587af83d2b507de449");
        repoManager.checkout();

        var extractor = extractor(testEnvironment(tempDir), false);
        var data = extractor.extractProjectData(repoManager.getRepoDir(), Set.of());
        assertThat(data.name)
                .isEqualTo("NeoForge-Root");
        var subProjectList = data.getData(SubProjectList.class);
        assertThat(subProjectList)
                .isNotNull();

        var nfSubProject = subProjectList.get("neoforge");
        assertThat(nfSubProject)
                .isNotNull();
        var nfSubProjectGradleData = nfSubProject.getData(PluginData.class);
        assertThat(nfSubProjectGradleData)
                .isNotNull();

        var neoDevData = nfSubProjectGradleData.getData(NeoDevData.class);
        assertThat(neoDevData)
                .isNotNull();

        assertThat(nfSubProject)
                .extracting(e -> e.getData(ConfigurationList.class))
                .isNotNull()
                .extracting(e -> e.get(neoDevData.moduleClasspathConfiguration))
                .isNotNull()
                .extracting(e -> e.dependencies.size())
                .isNotEqualTo(0);
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
