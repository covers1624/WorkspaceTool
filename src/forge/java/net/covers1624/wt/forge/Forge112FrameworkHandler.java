package net.covers1624.wt.forge;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.framework.FrameworkHandler;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.GradleModelCache;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ConfigurationImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.ModuleList;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.forge.api.script.Forge112;
import net.covers1624.wt.forge.util.ATMerger;
import net.covers1624.wt.util.GradleModuleModelHelper;
import net.covers1624.wt.util.HashContainer;
import net.covers1624.wt.util.MavenNotation;
import net.covers1624.wt.util.Utils;
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
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptySet;
import static net.covers1624.wt.util.Utils.sneaky;

/**
 * Created by covers1624 on 10/7/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class Forge112FrameworkHandler implements FrameworkHandler<Forge112> {

    private static final String GRADLE_VERSION = "4.10.3";
    private static final HashCode MARKER_HASH = HashCode.fromString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    private static final Logger logger = LogManager.getLogger("Forge112FrameworkHandler");
    private static final String HASH_BRANCH_COMMIT = "branch-commit";
    private static final String HASH_MERGED_AT = "merged-at";
    private static final String HASH_MAPPINGS_INFO = "mappings-info";
    private static final String HASH_MARKER_SETUP = "marker-setup";
    private static final String HASH_GSTART_LOGIN = "gstart-login";
    private static final HashFunction sha256 = Hashing.sha256();

    private static final String LOCAL_BRANCH_SUFFIX = "-wt-local";

    private final Path projectDir;
    private final Path cacheDir;
    private final GradleManager gradleManager;
    private final GradleModelCache modelCache;
    private final HashContainer hashContainer;

    private Path forgeDir;
    private boolean needsSetup;
    private String localBranchTarget;

    public Forge112FrameworkHandler(Path projectDir, Path cacheDir, GradleManager gradleManager, GradleModelCache modelCache) {
        this.projectDir = projectDir;
        this.cacheDir = cacheDir;
        this.gradleManager = gradleManager;
        this.modelCache = modelCache;
        hashContainer = new HashContainer(cacheDir.resolve("forge_framework_cache.json"));
        needsSetup = hashContainer.get(HASH_MARKER_SETUP) != null;
    }

    @Override
    public void constructFrameworkModules(Forge112 frameworkImpl, ModuleList moduleList) {
        forgeDir = projectDir.resolve(frameworkImpl.getPath());
        localBranchTarget = frameworkImpl.getBranch() + LOCAL_BRANCH_SUFFIX;
        sneaky(() -> validateForgeClone(frameworkImpl));

        Path mergedAT = cacheDir.resolve("merged_at.cfg");
        { //AccessTransformers.
            Hasher atHasher = sha256.newHasher();
            List<Path> atFiles = moduleList.modules.parallelStream()//
                    .flatMap(e -> e.getSourceSets().values().stream())//
                    .flatMap(e -> e.getResources().stream())//
                    .filter(Files::exists)//
                    .flatMap(e -> sneaky(() -> Files.walk(e)).filter(f -> f.getFileName().toString().endsWith("_at.cfg")))//
                    .collect(Collectors.toList());
            atFiles.forEach(e -> logger.info("Found AccessTransformer: {}", e));
            atFiles.forEach(e -> Utils.addToHasher(atHasher, e));
            HashCode atHash = atHasher.hash();
            if (hashContainer.check(HASH_MERGED_AT, atHash)) {
                needsSetup = true;
                hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
                ATMerger atMerger = new ATMerger();
                atFiles.forEach(atMerger::consume);
                atMerger.write(mergedAT);
                hashContainer.set(HASH_MERGED_AT, atHash);
            }
        }

        Path formsRt = cacheDir.resolve("libs/forms_rt.jar");
        Dependency formsRtDep = new MavenDependencyImpl()//
                .setNotation(MavenNotation.parse("org.jetbrains:forms_rt:1.0.0"))//
                .setClasses(formsRt).setExport(false);
        { //GStart Login.
            Path r1 = forgeDir.resolve("src/start/java/GradleStartLogin.java");
            Path r2 = forgeDir.resolve("src/start/java/net/covers1624/wt/gstart/CredentialsDialog.java");

            Hasher gStartLoginResourcesHasher = sha256.newHasher();
            Utils.addToHasher(gStartLoginResourcesHasher, "/gstart_login/forms_rt.jar");
            Utils.addToHasher(gStartLoginResourcesHasher, "/gstart_login/src/GradleStartLogin.java");
            Utils.addToHasher(gStartLoginResourcesHasher, "/gstart_login/src/net/covers1624/wt/gstart/CredentialsDialog.java");
            HashCode hash1 = gStartLoginResourcesHasher.hash();

            Hasher gStartLoginFilesHasher = sha256.newHasher();
            Utils.addToHasher(gStartLoginFilesHasher, formsRt);
            Utils.addToHasher(gStartLoginFilesHasher, r1);
            Utils.addToHasher(gStartLoginFilesHasher, r2);
            HashCode hash2 = gStartLoginFilesHasher.hash();

            if (hashContainer.check(HASH_GSTART_LOGIN, hash1) || !hash2.equals(hash1)) {
                Utils.extractResource("/gstart_login/forms_rt.jar", formsRt);
                Utils.extractResource("/gstart_login/src/GradleStartLogin.java", r1);
                Utils.extractResource("/gstart_login/src/net/covers1624/wt/gstart/CredentialsDialog.java", r2);
                hashContainer.set(HASH_GSTART_LOGIN, hash1);
            }
        }
        WorkspaceToolModel model = modelCache.getModel(forgeDir, emptySet());

        Module forgeModule = new ModuleImpl.GradleModule("Forge", "", forgeDir, model.getPluginData(), model.getGradleData());
        moduleList.frameworkModules.add(forgeModule);
        Map<String, Configuration> configurations = GradleModuleModelHelper.populateConfigurations(forgeModule, model.getGradleData());
        Configuration forgeGradleMcDeps = configurations.get("forgeGradleMcDeps");
        Configuration runtime = configurations.get("runtime");
        addSourceSet(forgeModule, "main", ss -> {
            ss.addSource("java", Arrays.asList(//
                    forgeDir.resolve("src/main/java"),// Forge's sources.
                    forgeDir.resolve("src/start/java"),// Generated GradleStartLogin from WorkspaceTool.
                    forgeDir.resolve("projects/Forge/src/main/java"),// Decompiled and patched Minecraft.
                    forgeDir.resolve("projects/Forge/src/main/start")// GradleStart.
            ));
            ss.addResource(forgeDir.resolve("src/main/resources")); // Forge resources.
            ss.addResource(forgeDir.resolve("projects/Forge/src/main/resources"));// MinecraftResources.
            Configuration forgeMainCompile = new ConfigurationImpl("forgeMainCompile", true);
            forgeMainCompile.addExtendsFrom(forgeGradleMcDeps);
            forgeMainCompile.addDependency(formsRtDep);
            ss.setCompileConfiguration(forgeMainCompile);
            ss.setRuntimeConfiguration(runtime);
        });
        moduleList.modules.forEach(m -> {
            Map<String, Configuration> cfgs = m.getConfigurations();
            SourceSet main = m.getSourceSets().get("main");
            Configuration deobfCompile = cfgs.get("deobfCompile");
            Configuration deobfProvided = cfgs.get("deobfProvided");
            Configuration compileConfiguration = main.getCompileConfiguration();
            Configuration compileOnlyConfiguration = main.getCompileOnlyConfiguration();
            if (compileConfiguration != null) {
                if (deobfCompile != null) {
                    compileConfiguration.addExtendsFrom(deobfCompile);
                }
                compileConfiguration.addDependency(new SourceSetDependencyImpl().setSourceSet("main").setModule(forgeModule));
            }
            if (compileOnlyConfiguration != null && deobfProvided != null) {
                compileOnlyConfiguration.addExtendsFrom(deobfProvided);
            }

            runtime.addDependency(new SourceSetDependencyImpl().setSourceSet("main").setModule(m).setExport(false));
        });

        if (needsSetup) {
            sneaky(() -> Files.copy(mergedAT, forgeDir.resolve("src/main/resources/wt_merged_at.cfg"), REPLACE_EXISTING));
            try (ProjectConnection connection = GradleConnector.newConnector()//
                    .useGradleVersion(GRADLE_VERSION)//
                    .forProjectDirectory(forgeDir.toFile())//
                    .connect()) {
                connection.newBuild()//
                        .setEnvironmentVariables(ImmutableMap.of(//
                                "GIT_BRANCH", "/" + localBranchTarget,//
                                "BUILD_NUMBER", "9999"//
                        ))//
                        .forTasks("clean", "ciWriteBuildNumber", "setupForge")//
                        .withArguments("-si")//
                        .setStandardOutput(System.out)//
                        .setStandardError(System.err)//
                        .run();
            }
            hashContainer.remove(HASH_MARKER_SETUP);//clear the marker.
        }
    }

    private SourceSet addSourceSet(Module module, String name, Consumer<SourceSet> func) {
        SourceSet sourceSet = new SourceSetImpl(name);
        module.addSourceSet(name, sourceSet);
        func.accept(sourceSet);
        return sourceSet;
    }

    private Configuration addConfiguration(Module module, String name, boolean transitive, Consumer<Configuration> func) {
        Configuration sourceSet = new ConfigurationImpl(name, transitive);
        module.addConfiguration(name, sourceSet);
        func.accept(sourceSet);
        return sourceSet;
    }

    private void validateForgeClone(Forge112 frameworkImpl) throws Throwable {
        ProgressMonitor pm = new TextProgressMonitor(new OutputStreamWriter(System.out, UTF_8));
        logger.info("Validating forge clone..");
        Path forgeGitDir = forgeDir.resolve(".git");

        Git git;
        Repository repo;
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(forgeGitDir.toFile()).readEnvironment();
        Repository tmpRepo = builder.build();
        if (!tmpRepo.getObjectDatabase().exists()) {
            logger.info("Forge clone missing or corrupt, Re-Cloning..");
            CloneCommand clone = Git.cloneRepository();
            clone.setBare(false).setCloneAllBranches(true);
            clone.setDirectory(forgeDir.toFile());
            clone.setProgressMonitor(pm);
            clone.setURI(frameworkImpl.getUrl());
            git = clone.call();
            repo = git.getRepository();
        } else {
            repo = tmpRepo;
            git = new Git(repo);
        }

        String currentBranch = repo.getBranch();
        String currentCommit = repo.resolve("HEAD").name();

        Hasher branchHasher = sha256.newHasher();
        branchHasher.putString(frameworkImpl.getBranch(), UTF_8);
        branchHasher.putString(frameworkImpl.getCommit(), UTF_8);
        HashCode branchHash = branchHasher.hash();

        boolean correctBranch = currentBranch.equals(localBranchTarget);
        boolean correctCommit = currentCommit.startsWith(frameworkImpl.getCommit());

        if (hashContainer.check(HASH_BRANCH_COMMIT, branchHash) || !correctBranch || !correctCommit) {
            logger.info("Branch or Commit changed, Checking out new Branch / Commit..");
            needsSetup = true;
            git.fetch().setProgressMonitor(pm).call();
            git.reset().setRef("HEAD").setMode(ResetCommand.ResetType.HARD).call();
            git.clean().setIgnore(false).setCleanDirectories(true).setForce(true).call();
            if (git.branchList().call().stream().noneMatch(e -> e.getName().equals("refs/heads/" + frameworkImpl.getBranch()))) {
                git.checkout()//
                        .setCreateBranch(true)//
                        .setName(frameworkImpl.getBranch())//
                        .setStartPoint("origin/" + frameworkImpl.getBranch())//
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)//
                        .call();
            } else {
                git.checkout().setName(frameworkImpl.getBranch()).call();
            }
            if (currentBranch.endsWith(LOCAL_BRANCH_SUFFIX)) {
                git.branchDelete().setBranchNames(currentBranch).setForce(true).call();
            }
            RevCommit checkout_start = repo.parseCommit(repo.resolve(frameworkImpl.getCommit()));
            git.checkout().setStartPoint(checkout_start).setCreateBranch(true).setName(localBranchTarget).call();
            hashContainer.set(HASH_BRANCH_COMMIT, branchHash);
            logger.info("Checked out new Branch / Commit.");
        }

        if (needsSetup) {
            hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
        }
        logger.info("Validated.");
    }
}
