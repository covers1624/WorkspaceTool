/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wstool.wrapper;

import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.jdkutils.locator.JavaLocator;
import net.covers1624.jdkutils.provisioning.adoptium.AdoptiumProvisioner;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.wstool.wrapper.http.Java8HttpEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by covers1624 on 20/8/20.
 */
public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Wrapper");

    public static final Path SYSTEM_WT_FOLDER = Paths.get(System.getProperty("user.home"), ".workspace_tool")
            .toAbsolutePath()
            .normalize();
    public static final Path WT_JDKS = SYSTEM_WT_FOLDER.resolve("jdks");
    public static final Path PROJECT_FOLDER = Paths.get(".")
            .toAbsolutePath()
            .normalize();
    public static final Path PROJECT_WT_FOLDER = PROJECT_FOLDER.resolve(".wstool");

    public static final String VERSION;

    static {
        Package pkg = Main.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        VERSION = version != null ? version : "0.0.0";
    }

    public static void main(String[] args) throws Throwable {
        System.exit(mainI(args));
    }

    private static int mainI(String[] args) throws Throwable {
        boolean attach = false;
        boolean local = false;
        for (String arg : args) {
            if (arg.equals("--attach")) {
                attach = true;
            }
            if (arg.equals("--local")) {
                local = true;
            }
        }

        Files.createDirectories(SYSTEM_WT_FOLDER);
        Files.createDirectories(PROJECT_WT_FOLDER);
        LOGGER.info("Preparing WorkspaceTool..");
        WrapperProperties properties = WrapperProperties.load(PROJECT_WT_FOLDER.resolve("wrapper.properties"));
        HttpEngine httpEngine = new Java8HttpEngine();
        RuntimeResolver resolver = new RuntimeResolver(httpEngine, properties, SYSTEM_WT_FOLDER.resolve("libraries"));

        RuntimeResolver.RuntimeEnvironment environment = resolver.resolve(local);
        if (environment == null) {
            return -1;
        }

        LOGGER.info("Preparing JRE..");
        Path jre = selectJre(httpEngine, environment.javaVersion);
        LOGGER.info("Launching..");

        System.out.println();
        System.out.println();

        List<String> command = new LinkedList<>();
        command.add(JavaInstall.getJavaExecutable(jre, false).toAbsolutePath().toString());
        if (attach) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005");
        }
        command.addAll(environment.javaArgs);
        ProcessBuilder builder = new ProcessBuilder()
                .inheritIO()
                .command(command);

        Process process = builder.start();
        process.waitFor();
        return 0;
    }

    private static Path selectJre(HttpEngine http, JavaVersion required) throws IOException {
        JavaLocator locator = JavaLocator.builder()
                .findGradleJdks()
                .findIntellijJdks()
                .ignoreOpenJ9()
                .filter(required)
                .build();

        List<JavaInstall> installs = locator.findJavaVersions();
        if (!installs.isEmpty()) return installs.get(0).javaHome;

        JdkInstallationManager installManager = new JdkInstallationManager(WT_JDKS, new AdoptiumProvisioner(http));

        return installManager.provisionJdk(new JdkInstallationManager.ProvisionRequest.Builder()
                .forVersion(required)
                .build()
        );
    }
}
