package net.covers1624.wstool.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 1/14/25.
 */
public class GitRepoManagerTests {

    @Test
    public void testCheckout(@TempDir Path tempDir) throws IOException {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("repo"));
        repoManager.setConfig("https://github.com/TheCBProject/CodeChickenLib.git", "1.20.1", "10905a968d82598ce4ac5abead1a91c238b4e112");
        assertTrue(repoManager.checkout());
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("build.gradle")));
        assertFalse(repoManager.checkout());
    }

    @Test
    public void testCheckoutSwitchCommit(@TempDir Path tempDir) throws IOException {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("repo"));
        repoManager.setConfig("https://github.com/TheCBProject/CodeChickenLib.git", "1.20.1", "10905a968d82598ce4ac5abead1a91c238b4e112");
        assertTrue(repoManager.checkout());
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("build.gradle")));
        repoManager.setConfig("https://github.com/TheCBProject/CodeChickenLib.git", "1.20.1", "83b1a01c96f781eca59ca900caf660e1b83ba29e");
        assertTrue(repoManager.checkout());
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("build.gradle")));
        assertFalse(repoManager.checkout());
    }

    @Test
    public void testCheckoutSwitchCommitWithChanges(@TempDir Path tempDir) throws IOException {
        GitRepoManager repoManager = new GitRepoManager(tempDir.resolve("repo"));
        repoManager.setConfig("https://github.com/TheCBProject/CodeChickenLib.git", "1.20.1", "10905a968d82598ce4ac5abead1a91c238b4e112");
        assertTrue(repoManager.checkout());
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("build.gradle")));
        // Delete, change and add some files.
        Files.delete(repoManager.getRepoDir().resolve("build.gradle"));
        Files.writeString(repoManager.getRepoDir().resolve("settings.gradle"), "ASDF");
        Files.writeString(repoManager.getRepoDir().resolve("asdf.txt"), "ASDFASDFASDFASDF");
        Files.writeString(repoManager.getRepoDir().resolve("src/asdf.txt"), "ASDFASDFASDFASDF");
        assertFalse(repoManager.checkout()); // Changes should be ignored, no branch/commit change was done.
        // Checkout a different commit.
        repoManager.setConfig("https://github.com/TheCBProject/CodeChickenLib.git", "1.20.1", "83b1a01c96f781eca59ca900caf660e1b83ba29e");
        assertTrue(repoManager.checkout());
        // Everything should be reset, except the untracked asdf.txt file.
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("build.gradle")));
        assertNotEquals("ASDF", Files.readString(repoManager.getRepoDir().resolve("settings.gradle")));
        assertTrue(Files.exists(repoManager.getRepoDir().resolve("asdf.txt"))); // File is untracked, should remain.
        assertFalse(Files.exists(repoManager.getRepoDir().resolve("src/asdf.txt")));
        assertFalse(repoManager.checkout());
    }
}
