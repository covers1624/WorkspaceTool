/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.OutputStreamWriter;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by covers1624 on 7/8/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class GitHelper {

    private static final Logger logger = LogManager.getLogger("GitHelper");
    private static final String HASH_BRANCH_COMMIT = "branch-commit";
    private static final HashFunction sha256 = Hashing.sha256();

    private final HashContainer hashContainer;
    private String repoUrl;
    private Path path;
    private String branch;
    private String commit;
    private String branchSuffix;

    public GitHelper(HashContainer hashContainer) {
        this.hashContainer = hashContainer;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public void setBranchSuffix(String branchSuffix) {
        this.branchSuffix = branchSuffix;
    }

    public boolean validate() throws Throwable {
        String localBranchTarget = branch + branchSuffix;
        ProgressMonitor pm = new TextProgressMonitor(new OutputStreamWriter(System.out, UTF_8));
        logger.info("Validating clone of '{}' ..", repoUrl);

        Git git;
        Repository repo;
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(path.resolve(".git").toFile()).readEnvironment();
        Repository tmpRepo = builder.build();
        if (!tmpRepo.getObjectDatabase().exists()) {
            logger.info("Clone missing or corrupt, Re-Cloning..");
            CloneCommand clone = Git.cloneRepository();
            clone.setBare(false).setCloneAllBranches(true);
            clone.setDirectory(path.toFile());
            clone.setProgressMonitor(pm);
            clone.setURI(repoUrl);
            git = clone.call();
            repo = git.getRepository();
        } else {
            repo = tmpRepo;
            git = new Git(repo);
        }

        String currentBranch = repo.getBranch();
        String currentCommit = repo.resolve("HEAD").name();

        Hasher branchHasher = sha256.newHasher();
        branchHasher.putString(branch, UTF_8);
        branchHasher.putString(commit, UTF_8);
        HashCode branchHash = branchHasher.hash();

        boolean correctBranch = currentBranch.equals(localBranchTarget);
        boolean correctCommit = currentCommit.startsWith(commit);

        boolean didStuff = false;
        if (hashContainer.check(HASH_BRANCH_COMMIT, branchHash) || !correctBranch || !correctCommit) {
            logger.info("Branch or Commit changed, Checking out new Branch / Commit..");
            git.fetch().setProgressMonitor(pm).call();
            git.reset().setRef("HEAD").setMode(ResetCommand.ResetType.HARD).call();
            git.clean().setIgnore(false).setCleanDirectories(true).setForce(true).call();
            if (git.branchList().call().stream().noneMatch(e -> e.getName().equals("refs/heads/" + branch))) {
                git.checkout()
                        .setCreateBranch(true)
                        .setName(branch)
                        .setStartPoint("origin/" + branch)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .call();
            } else {
                git.checkout().setName(branch).call();
            }
            if (currentBranch.endsWith(branchSuffix)) {
                git.branchDelete().setBranchNames(currentBranch).setForce(true).call();
            }
            RevCommit checkout_start = repo.parseCommit(repo.resolve(commit));
            git.checkout().setStartPoint(checkout_start).setCreateBranch(true).setName(localBranchTarget).call();
            hashContainer.set(HASH_BRANCH_COMMIT, branchHash);
            logger.info("Checked out new Branch / Commit.");
            didStuff = true;
        }
        logger.info("Validated.");
        return didStuff;
    }

}
