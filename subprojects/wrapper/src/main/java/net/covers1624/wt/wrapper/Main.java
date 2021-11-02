package net.covers1624.wt.wrapper;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.wrapper.java.JDKManager;
import net.covers1624.wt.wrapper.java.JavaInstall;
import net.covers1624.wt.wrapper.java.JavaUtils;
import net.covers1624.wt.wrapper.json.JDKProperties;
import net.covers1624.wt.wrapper.json.JsonUtils;
import net.covers1624.wt.wrapper.json.WrapperProperties;
import net.covers1624.wt.wrapper.maven.MavenResolver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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

    public static final Path SYSTEM_WT_FOLDER = Paths.get(System.getProperty("user.home"), ".workspace_tool")
            .normalize().toAbsolutePath();
    public static final Path WT_JDKS = SYSTEM_WT_FOLDER.resolve("jdks");
    private static final List<String> YES = Arrays.asList("y", "yes");
    private static final List<String> NO = Arrays.asList("n", "no");

    public static void main(String[] args) throws Throwable {
        Files.createDirectories(SYSTEM_WT_FOLDER);
        Files.createDirectories(Paths.get("./.workspace_tool"));
        LOGGER.info("Preparing WorkspaceTool..");
        Path workspacePropsFile = Paths.get(".workspace_tool/properties.json");
        WrapperProperties workspaceProps = WrapperProperties.compute(workspacePropsFile);

        Path jdkPropsFile = Paths.get(".workspace_tool/jdk.json");
        JDKProperties jdkProps;
        if (Files.exists(jdkPropsFile)) {
            jdkProps = JsonUtils.parse(jdkPropsFile, JDKProperties.class);
        } else {
            jdkProps = new JDKProperties();
        }
        if (jdkProps.selected == null || Files.notExists(jdkProps.selected)) {
            jdkProps.selected = computeJDK(workspaceProps);
            JsonUtils.write(jdkPropsFile, jdkProps);
        }

        MavenResolver resolver = new MavenResolver(SYSTEM_WT_FOLDER.resolve("local_repo"), workspaceProps.repos);
        List<Path> deps = resolver.resolve(MavenNotation.parse(workspaceProps.artifact));

        System.out.println();
        System.out.println();
        ProcessBuilder builder = new ProcessBuilder()
                .inheritIO()
                .command(
                        JavaUtils.getJavaExecutable(jdkProps.selected).toAbsolutePath().toString(),
                        "-cp",
                        deps.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)),
                        workspaceProps.mainClass
                );

        Process process = builder.start();
        process.waitFor();
    }

    private static Path computeJDK(WrapperProperties props) throws IOException {
        LOGGER.info("WorkspaceTool has no configured JDK. Searching known java paths for JDK's...");
        List<JavaInstall> javaInstalls = JavaUtils.locateExistingInstalls(props.requiredJava);
        if (javaInstalls.isEmpty()) {
            JDKManager jdkManager = new JDKManager(WT_JDKS);
            Path jdkFind = jdkManager.findJDK(props.requiredJava);
            if (jdkFind != null) {
                LOGGER.info("Selected JDK: {}", jdkFind);
                return jdkFind;
            }
            LOGGER.info("WorkspaceTool could not find any compatible {} JDKs installed.", props.requiredJava);
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
            Path javaHome = jdkManager.installJdk(props.requiredJava);
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
        System.out.print("Please select. [0-" + (javaInstalls.size() - 1) + "]: ");
        Scanner scanner = new Scanner(System.in);
        String next = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
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
}
