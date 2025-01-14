package net.covers1624.wstool.api;

import net.covers1624.quack.collection.FastStream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by covers1624 on 1/13/25.
 */
public class GitRepoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepoManager.class);

    private final Path repoDir;

    private @Nullable RepoConfig config;

    public GitRepoManager(Path repoDir) {
        this.repoDir = repoDir;
    }

    public GitRepoManager setConfig(String url, String branch, String commit) {
        config = new RepoConfig(url, branch, commit);

        return this;
    }

    public Path getRepoDir() {
        return repoDir;
    }

    /**
     * Checkout the configured branch and commit.
     * <p>
     * Any changes in the working directory will be voided.
     *
     * @return If any actions were performed.
     */
    public boolean checkout() throws IOException {
        if (config == null) throw new RuntimeException("Not configured,");

        LOGGER.info("Validating clone of {} in {}", config.url, repoDir);
        ProgressMonitor pm = new TextProgressMonitor(new OutputStreamWriter(System.out, UTF_8));

        Git git;
        if (Files.notExists(repoDir.resolve(".git/objects"))) {
            try {
                git = Git.init()
                        .setDirectory(repoDir.toFile())
                        .call();
            } catch (GitAPIException ex) {
                throw new RuntimeException("Failed to initialize git repo.", ex);
            }
        } else {
            git = Git.open(repoDir.toFile());
        }
        Repository repo = git.getRepository();
        RevWalk revWalk = new RevWalk(repo);

        try (git; revWalk) {
            RemoteConfig remote = FastStream.of(git.remoteList().call())
                    .filter(e -> e.getName().equals("origin"))
                    .firstOrDefault();
            URIish remoteUri = remote != null && !remote.getURIs().isEmpty() ? remote.getURIs().get(0) : null;
            if (remoteUri == null || !config.url.equals(remoteUri.toString())) {
                LOGGER.info("Setting 'origin' to '{}'", config.url);
                git.remoteRemove()
                        .setRemoteName("origin")
                        .call();
                git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(config.url))
                        .call();
            }

            String currentBranch = repo.getBranch();
            ObjectId currentCommitObject = repo.resolve("HEAD");
            String currentCommit = currentCommitObject != null ? currentCommitObject.name() : null;

            boolean correctBranch = config.branch.equals(currentBranch);
            boolean correctCommit = config.commit.equals(currentCommit);
            if (correctBranch && correctCommit) {
                LOGGER.info("Validated. No changes.");
                return false;
            }

            LOGGER.info("Commit or branch changed.");
            LOGGER.info("Fetching remote changes..");
            git.fetch()
                    .setProgressMonitor(pm)
                    .call();

            ObjectId commitObject = repo.resolve(config.commit);
            if (commitObject == null) {
                throw new RuntimeException("Commit " + config.commit + " does not exist.");
            }

            boolean branchExists = repo.findRef(Constants.R_HEADS + config.branch) != null;

            if (!branchExists) {
                LOGGER.info("Checking out new branch {} at {}.", config.branch, config.commit);
                git.checkout()
                        .setCreateBranch(true)
                        .setName(config.branch)
                        .setStartPoint(revWalk.parseCommit(commitObject))
                        .call();
            } else {
                LOGGER.info("Resetting current branch to HEAD..");
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .call();
                git.clean()
                        .setForce(true)
                        .setCleanDirectories(true)
                        .call();

                if (!correctBranch) {
                    LOGGER.info("Switching to existing branch {}.", config.branch);
                    git.checkout()
                            .setName(config.branch)
                            .call();
                }

                LOGGER.info("Resetting branch to commit {}.", config.commit);
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef(config.commit)
                        .call();
            }
            LOGGER.info("Validated. Commit/branch changed.");
            return true;
        } catch (GitAPIException | URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    private record RepoConfig(String url, String branch, String commit) { }
}
