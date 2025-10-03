package net.covers1624.wstool.minecraft;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by covers1624 on 10/3/25.
 */
public class DevLoginProcessor {

    private static final MavenNotation DEVLOGIN_NOTATION = MavenNotation.parse("net.covers1624:DevLogin:0.1.0.5");

    private final HttpEngine http;
    private final Path librariesDir;
    private @Nullable Path devLoginJar;

    public DevLoginProcessor(Environment env) {
        http = env.getService(HttpEngine.class);
        librariesDir = env.systemFolder().resolve("libraries");
    }

    public void processWorkspace(Workspace workspace) {
        for (RunConfig config : workspace.runConfigs().values()) {
            attachDevLogin(config);
        }
    }

    private void attachDevLogin(RunConfig runConfig) {
        String devLoginProfile = runConfig.config().get("dev_login");
        String mainClass = runConfig.mainClass();
        if (devLoginProfile == null || mainClass == null) return;
        addDependency(runConfig);

        runConfig.sysProps().putFirst("devlogin.launch_profile", devLoginProfile);
        runConfig.sysProps().putFirst("devlogin.launch_target", mainClass);
        runConfig.mainClass("net.covers1624.devlogin.DevLogin");
    }

    private void addDependency(RunConfig config) {
        Path devLoginJar = getDevLoginJar();
        var classpath = config.classpath();
        if (classpath == null) return;

        for (Dependency runtimeDependency : classpath.runtimeDependencies()) {
            if (runtimeDependency instanceof Dependency.MavenDependency mavenDep) {
                if (devLoginJar.equals(mavenDep.files().get("classes"))) return;
            }
        }
        classpath.runtimeDependencies()
                .add(new Dependency.MavenDependency(
                        DEVLOGIN_NOTATION,
                        Map.of("classes", devLoginJar)
                ));
    }

    private Path getDevLoginJar() {
        if (devLoginJar == null) {
            devLoginJar = DEVLOGIN_NOTATION.toPath(librariesDir);

            try {
                new HttpEngineDownloadAction()
                        .setUrl(DEVLOGIN_NOTATION.toURL("https://proxy-maven.covers1624.net/").toString())
                        .setDest(devLoginJar)
                        .setUseETag(true)
                        .setEngine(http)
                        .execute();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to download DevLogin.", ex);
            }
        }

        return devLoginJar;
    }
}
