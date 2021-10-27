package net.covers1624.wt.forge;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.dependency.SourceSetDependencyImpl;
import net.covers1624.wt.api.impl.module.ConfigurationImpl;
import net.covers1624.wt.api.impl.module.ModuleImpl;
import net.covers1624.wt.api.impl.module.SourceSetImpl;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.SourceSet;
import net.covers1624.wt.forge.api.script.Forge112;
import net.covers1624.wt.forge.util.AtFile;
import net.covers1624.wt.util.ProjectDataHelper;
import net.covers1624.wt.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.of;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static net.covers1624.wt.util.Utils.sneaky;

/**
 * Created by covers1624 on 10/7/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class Forge112FrameworkHandler extends AbstractForgeFrameworkHandler<Forge112> {

    public Forge112FrameworkHandler(WorkspaceToolContext context) {
        super(context);
    }

    @Override
    public void constructFrameworkModules(Forge112 frameworkImpl) {
        super.constructFrameworkModules(frameworkImpl);
        Path mergedAT = context.cacheDir.resolve("merged_at.cfg");
        { //AccessTransformers.
            Hasher atHasher = SHA_256.newHasher();
            List<Path> atFiles = context.modules.parallelStream()
                    .flatMap(e -> e.getSourceSets().values().stream())
                    .flatMap(e -> e.getResources().stream())
                    .filter(Files::exists)
                    .flatMap(e -> sneaky(() -> Files.walk(e)).filter(f -> f.getFileName().toString().endsWith("_at.cfg")))
                    .collect(Collectors.toList());
            atFiles.forEach(e -> LOGGER.info("Found AccessTransformer: {}", e));
            atFiles.forEach(e -> Utils.addToHasher(atHasher, e));
            HashCode atHash = atHasher.hash();
            if (hashContainer.check(HASH_MERGED_AT, atHash)) {
                needsSetup = true;
                hashContainer.set(HASH_MARKER_SETUP, MARKER_HASH);
                AtFile atFile = new AtFile();
                atFiles.stream().map(AtFile::new).forEach(atFile::merge);
                atFile.write(mergedAT);
                hashContainer.set(HASH_MERGED_AT, atHash);
            }
        }

        Path formsRt = context.cacheDir.resolve("libs/forms_rt.jar");
        Dependency formsRtDep = new MavenDependencyImpl()
                .setNotation(MavenNotation.parse("org.jetbrains:forms_rt:1.0.0"))
                .setClasses(formsRt).setExport(false);
        { //GStart Login.
            Path r1 = forgeDir.resolve("src/start/java/GradleStartLogin.java");
            Path r2 = forgeDir.resolve("src/start/java/net/covers1624/wt/gstart/CredentialsDialog.java");

            Hasher gStartLoginResourcesHasher = SHA_256.newHasher();
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/forms_rt.jar");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/112/src/GradleStartLogin.java");
            Utils.addToHasher(gStartLoginResourcesHasher, "/wt_login/112/src/net/covers1624/wt/gstart/CredentialsDialog.java");
            HashCode hash1 = gStartLoginResourcesHasher.hash();

            Hasher gStartLoginFilesHasher = SHA_256.newHasher();
            Utils.addToHasher(gStartLoginFilesHasher, formsRt);
            Utils.addToHasher(gStartLoginFilesHasher, r1);
            Utils.addToHasher(gStartLoginFilesHasher, r2);
            HashCode hash2 = gStartLoginFilesHasher.hash();

            if (hashContainer.check(HASH_GSTART_LOGIN, hash1) || !hash2.equals(hash1)) {
                Utils.extractResource("/wt_login/forms_rt.jar", formsRt);
                Utils.extractResource("/wt_login/112/src/GradleStartLogin.java", r1);
                Utils.extractResource("/wt_login/112/src/net/covers1624/wt/gstart/CredentialsDialog.java", r2);
                hashContainer.set(HASH_GSTART_LOGIN, hash1);
            }
        }
        WorkspaceToolModel model = context.modelCache.getModel(forgeDir, emptySet(), emptySet());

        Module forgeModule = new ModuleImpl.GradleModule("Forge", forgeDir, model.getProjectData());
        context.frameworkModules.add(forgeModule);
        Map<String, Configuration> configurations = ProjectDataHelper.buildConfigurations(forgeModule, model.getProjectData(), emptyMap());
        forgeModule.setConfigurations(configurations);
        Configuration forgeGradleMcDeps = configurations.get("forgeGradleMcDeps");
        Configuration runtime = configurations.get("runtime");
        addSourceSet(forgeModule, "main", ss -> {
            ss.setSource("java", Arrays.asList(
                    forgeDir.resolve("src/main/java"),// Forge's sources.
                    forgeDir.resolve("src/start/java"),// Generated GradleStartLogin from WorkspaceTool.
                    forgeDir.resolve("projects/Forge/src/main/java"),// Decompiled and patched Minecraft.
                    forgeDir.resolve("projects/Forge/src/main/start")// GradleStart.
            ));
            ss.addResource(forgeDir.resolve("src/main/resources")); // Forge resources.
            ss.addResource(forgeDir.resolve("projects/Forge/src/main/resources"));// MinecraftResources.
            Configuration forgeMainCompile = new ConfigurationImpl("forgeMainCompile");
            forgeMainCompile.addExtendsFrom(forgeGradleMcDeps);
            forgeMainCompile.addDependency(formsRtDep);
            ss.setCompileConfiguration(forgeMainCompile);
            ss.setRuntimeConfiguration(runtime);
        });
        context.modules.forEach(m -> {
            SourceSet main = m.getSourceSets().get("main");
            Configuration compileConfiguration = main.getCompileConfiguration();
            if (compileConfiguration != null) {
                compileConfiguration.addDependency(new SourceSetDependencyImpl(forgeModule, "main"));
            }

            runtime.addDependency(new SourceSetDependencyImpl(m, "main").setExport(false));
        });

        if (needsSetup) {
            sneaky(() -> Files.copy(mergedAT, forgeDir.resolve("src/main/resources/wt_merged_at.cfg"), REPLACE_EXISTING));
            runForgeSetup(of(
                            "GIT_BRANCH", "/" + frameworkImpl.getBranch() + LOCAL_BRANCH_SUFFIX,
                            "BUILD_NUMBER", "9999"),
                    "clean", "ciWriteBuildNumber", "setupForge");
            hashContainer.remove(HASH_MARKER_SETUP);//clear the marker.
        }
    }

    private SourceSet addSourceSet(Module module, String name, Consumer<SourceSet> func) {
        SourceSet sourceSet = new SourceSetImpl(name);
        module.addSourceSet(name, sourceSet);
        func.accept(sourceSet);
        return sourceSet;
    }
}
