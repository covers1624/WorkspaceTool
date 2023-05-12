/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper;

import com.google.gson.Gson;
import net.covers1624.jdkutils.*;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.java.JavaDownloadAction;
import net.covers1624.wt.wrapper.json.JDKProperties;
import net.covers1624.wt.wrapper.json.WrapperProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 20/8/20.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Wrapper");
    private static final Gson GSON = new Gson();

    public static final Path SYSTEM_WT_FOLDER = Paths.get(System.getProperty("user.home"), ".workspace_tool")
            .normalize().toAbsolutePath();
    public static final Path WT_JDKS = SYSTEM_WT_FOLDER.resolve("jdks");
    private static final List<String> YES = Arrays.asList("y", "yes");
    private static final List<String> NO = Arrays.asList("n", "no");

    public static void main(String[] args) throws Throwable {
        boolean attach = false;
        boolean localOnly = false;
        for (String arg : args) {
            if (arg.equals("--local-only")) {
                localOnly = true;
            }
            if (arg.equals("--attach")) {
                attach = true;
            }
        }

        Files.createDirectories(SYSTEM_WT_FOLDER);
        Files.createDirectories(Paths.get("./.workspace_tool"));
        LOGGER.info("Preparing WorkspaceTool..");
        Path workspacePropsFile = Paths.get(".workspace_tool/properties.json");
        WrapperProperties workspaceProps = WrapperProperties.load(workspacePropsFile);
        RuntimeResolver resolver = new RuntimeResolver(SYSTEM_WT_FOLDER.resolve("libraries"), workspaceProps);
        RuntimeResolver.RuntimeEnvironment environment = resolver.resolve(localOnly);

        Path jdkPropsFile = Paths.get(".workspace_tool/jdk.json");
        JDKProperties jdkProps;
        if (Files.exists(jdkPropsFile)) {
            jdkProps = JsonUtils.parse(GSON, jdkPropsFile, JDKProperties.class);
        } else {
            jdkProps = new JDKProperties();
        }
        Path selected = jdkProps.selected != null ? Paths.get(jdkProps.selected) : null;
        if (!isValidJDK(selected, environment.javaVersion)) {
            selected = computeJDK(environment.javaVersion);
            jdkProps.selected = selected.toString();
            JsonUtils.write(GSON, jdkPropsFile, jdkProps);
        }

        System.out.println();
        System.out.println();

        List<String> command = new LinkedList<>();
        command.add(JavaInstall.getJavaExecutable(selected, false).toAbsolutePath().toString());
        if (attach) {
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005");
        }
        command.add("-cp");
        command.add(environment.dependencies.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
        command.add(environment.mainClass);
        ProcessBuilder builder = new ProcessBuilder()
                .inheritIO()
                .command(command);

        Process process = builder.start();
        process.waitFor();
    }

    private static Path computeJDK(JavaVersion requiredJava) throws IOException {
        LOGGER.info("WorkspaceTool has no valid {} JDK configured. Searching for already provisioned jdk..", requiredJava);
        JdkInstallationManager jdkManager = new JdkInstallationManager(
                WT_JDKS,
                new AdoptiumProvisioner(JavaDownloadAction::new),
                false
        );
        Path jdkFind = jdkManager.findJdk(requiredJava, true);
        if (jdkFind != null) {
            LOGGER.info("Selected existing JDK: {}", jdkFind);
            return jdkFind;
        }
        LOGGER.info("Not found.. Searching common paths..");

        JavaLocator locator = JavaLocator.builder()
                .filter(requiredJava)
                .findGradleJdks()
                .findIntellijJdks()
                .ignoreOpenJ9()
                .build();

        List<JavaInstall> javaInstalls = locator.findJavaVersions();
        if (javaInstalls.isEmpty()) {
            LOGGER.info("WorkspaceTool could not find any compatible {} JDKs installed.", requiredJava);
            LOGGER.info(" WorkspaceTool can download a compatible Java JDK from https://adoptium.net");
            LOGGER.info(" Alternatively, you can install a compatible java JDK for your system, and re-run the tool.");
            System.out.print("Would you like to continue with downloading a compatible JDK (y/N)? ");
            Scanner scanner = new Scanner(System.in);
            String next = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (next.isEmpty() || NO.contains(next)) {
                LOGGER.info("Aborting.");
                System.exit(-1);
                return null;
            }
            if (!YES.contains(next)) {
                LOGGER.info("Unknown response '{}', aborting.", next);
                System.exit(-1);
                return null;
            }
            LOGGER.info("Finding compatible JDK on https://adoptium.net");
            Path javaHome = jdkManager.provisionJdk(requiredJava, true, new StatusDownloadListener());
            LOGGER.info("Selected JDK: {}", javaHome);
            return javaHome;
        }
        if (javaInstalls.size() == 1) {
            JavaInstall install = javaInstalls.get(0);
            LOGGER.info("Selected JDK: {}", install.javaHome);
            return install.javaHome;
        }
        LOGGER.info("WorkspaceTool identified multiple compatible JDK's. Please select:");
        int maxNumChars = String.valueOf(javaInstalls.size()).length();
        int maxJavaHomeChars = javaInstalls.stream().map(e -> e.javaHome.toAbsolutePath().toString().length()).max(Comparator.comparingInt(e -> e)).orElse(0);
        for (int i = 0; i < javaInstalls.size(); i++) {
            JavaInstall javaInstall = javaInstalls.get(i);
            System.out.println(
                    StringUtils.leftPad(String.valueOf(i), maxNumChars, ' ')
                            + ") "
                            + StringUtils.rightPad(javaInstall.javaHome.toAbsolutePath().toString(), maxJavaHomeChars, ' ')
                            + " " + javaInstall.implVersion
            );
        }
        System.out.println("d) To reject all these JDK's and download one from https://adoptium.net");
        System.out.print("Please select. [0-" + (javaInstalls.size() - 1) + "/d]: ");
        Scanner scanner = new Scanner(System.in);
        String next = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (next.equals("d")) {
            Path javaHome = jdkManager.provisionJdk(requiredJava, true, new StatusDownloadListener());
            LOGGER.info("Selected JDK: {}", javaHome);
            return javaHome;
        }
        if (!NumberUtils.isDigits(next)) {
            LOGGER.error("'{}' is not a number. Aborting..", next);
            System.exit(1);
            return null;
        }
        int selection = Integer.parseInt(next);
        if (selection < 0 || selection >= javaInstalls.size()) {
            LOGGER.error("'{}' is not within range. Aborting..", next);
            System.exit(1);
            return null;
        }
        JavaInstall selectedInstall = javaInstalls.get(selection);
        LOGGER.info("Selected JDK: {}", selectedInstall.javaHome);
        return selectedInstall.javaHome;
    }

    private static boolean isValidJDK(@Nullable Path home, JavaVersion target) {
        if (home == null || Files.notExists(home)) return false;

        JavaInstall install = JavaLocator.parseInstall(JavaInstall.getJavaExecutable(home, false));
        if (install == null) return false;

        return install.langVersion == target;
    }
}
