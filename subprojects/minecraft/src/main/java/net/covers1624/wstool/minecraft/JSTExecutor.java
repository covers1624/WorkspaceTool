package net.covers1624.wstool.minecraft;

import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.JdkProvider;
import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.SourceSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by covers1624 on 10/1/25.
 */
public class JSTExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSTExecutor.class);

    private static final MavenNotation JST_NOTATION = MavenNotation.parse("net.neoforged.jst:jst-cli-bundle:2.0.3");

    private final HttpEngine http;
    private final JdkProvider provider;
    private final Path librariesDir;

    private @Nullable Path jstBundlePath;

    public JSTExecutor(Environment env) {
        http = env.getService(HttpEngine.class);
        provider = env.getService(JdkProvider.class);
        librariesDir = env.systemFolder().resolve("libraries");
    }

    public void applyJST(SourceSet classpath, List<Path> mcSources, List<Path> ifaceInjections, List<Path> accessTransformers, @Nullable String parchmentVersion) {
        if (ifaceInjections.isEmpty() && accessTransformers.isEmpty() && parchmentVersion == null) return;

        var jst = getJstBundle();
        var javaHome = provider.findOrProvisionJdk(JavaVersion.JAVA_21);
        var librariesList = createLibrariesList(classpath, mcSources);

        List<String> args = new ArrayList<>();
        args.add(JavaInstall.getJavaExecutable(javaHome, true).toAbsolutePath().toString());
        args.add("-jar");
        args.add(jst.toAbsolutePath().toString());

        args.add("--libraries-list");
        args.add(librariesList.toAbsolutePath().toString());

        if (!ifaceInjections.isEmpty()) {
            args.add("--enable-interface-injection");
            for (Path file : ifaceInjections) {
                args.add("--interface-injection-data");
                args.add(file.toAbsolutePath().toString());
            }
        }

        if (!accessTransformers.isEmpty()) {
            args.add("--enable-accesstransformers");
            for (Path file : accessTransformers) {
                args.add("--access-transformer");
                args.add(file.toAbsolutePath().toString());
            }
        }

        if (parchmentVersion != null) {
            args.add("--enable-parchment");
            args.add("--parchment-conflict-prefix");
            args.add("p_");
            args.add("--parchment-mappings");
            args.add(downloadParchment(parchmentVersion).toAbsolutePath().toString());
        }

        Path inputZip = null;
        Path outputZip = null;
        // Fast path, we don't need to zip things.
        if (mcSources.size() == 1) {
            // Input and output are identical.
            args.add(mcSources.getFirst().toAbsolutePath().toString());
            args.add(mcSources.getFirst().toAbsolutePath().toString());
        } else {
            try {
                inputZip = makeSourceZip(mcSources);
                outputZip = Files.createTempFile("jst-output", "zip");
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create jst input/output zips.", ex);
            }

            args.add("--in-format");
            args.add("ARCHIVE");
            args.add("--out-format");
            args.add("ARCHIVE");
            args.add(inputZip.toAbsolutePath().toString());
            args.add(outputZip.toAbsolutePath().toString());
        }

        LOGGER.info("Running JST for {} iface injections and {} at's.", ifaceInjections.size(), accessTransformers.size());

        try {
            var proc = new ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                reader.lines().forEach(LOGGER::info);
            }
            proc.waitFor();
            if (proc.exitValue() != 0) {
                throw new RuntimeException("Failed to run JST. Exit code: " + proc.exitValue());
            }

            if (outputZip != null) {
                extractSourceZip(outputZip, mcSources);
            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Failed to run JST.", ex);
        } finally {
            try {
                Files.deleteIfExists(librariesList);
            } catch (IOException ignored) { }
            try {
                if (inputZip != null) Files.deleteIfExists(inputZip);
            } catch (IOException ignored) { }
            try {
                if (outputZip != null) Files.deleteIfExists(outputZip);
            } catch (IOException ignored) { }
        }
    }

    private static Path createLibrariesList(SourceSet sourceSet, List<Path> mcSources) {
        var classpath = collectClasspath(sourceSet);
        mcSources.forEach(classpath::remove);

        try {
            Path file = Files.createTempFile("jst-libs", "txt");
            file.toFile().deleteOnExit();
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
                for (Path path : classpath) {
                    if (!Files.exists(path)) continue;

                    pw.println(path.toAbsolutePath());
                }
            }
            return file;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write libraries list.", ex);
        }
    }

    private static Set<Path> collectClasspath(SourceSet ss) {
        Set<Path> classpath = new LinkedHashSet<>();
        ss.sourcePaths().forEach((n, v) -> classpath.addAll(v));
        for (Dependency mainDep : ss.compileDependencies()) {
            switch (mainDep) {
                case Dependency.MavenDependency mDep -> {
                    Path classes = mDep.files().get("classes");
                    if (classes != null) {
                        classpath.add(classes);
                    }
                }
                case Dependency.SourceSetDependency sDep -> classpath.addAll(collectClasspath(sDep.sourceSet()));
                case Dependency.FileDependency fDep -> classpath.add(fDep.file());
            }
        }
        return classpath;
    }

    private static Path makeSourceZip(List<Path> inputs) throws IOException {
        Path zip = Files.createTempFile("jst-input", "zip");
        zip.toFile().deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Path input : inputs) {
                Files.walkFileTree(input, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        zos.putNextEntry(new ZipEntry(input.relativize(file).toString()));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        return zip;
    }

    // Assumes JST will never create new files.
    private static void extractSourceZip(Path jstOutputZip, List<Path> inputs) throws IOException {
        try (ZipFile zipFile = new ZipFile(jstOutputZip.toFile())) {
            for (Path input : inputs) {
                Files.walkFileTree(input, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        var rel = input.relativize(file).toString();
                        var entry = zipFile.getEntry(rel);
                        if (entry == null) {
                            LOGGER.error("JST deleted input file {}", rel);
                            return FileVisitResult.CONTINUE;
                        }

                        try (var is = zipFile.getInputStream(entry)) {
                            Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    private Path getJstBundle() {
        if (jstBundlePath == null) {
            jstBundlePath = downloadMaven(JST_NOTATION);
        }
        return jstBundlePath;
    }

    private Path downloadParchment(String versionStr) {
        var split = versionStr.split("-");
        if (split.length != 2) throw new RuntimeException("Expected parchment version to be <mc>-<parchment> got: " + versionStr);

        return downloadMaven(new MavenNotation(
                "org.parchmentmc.data",
                "parchment-" + split[0],
                split[1],
                null,
                "zip"
        ));
    }


    private Path downloadMaven(MavenNotation notation) {
        Path dest = notation.toPath(librariesDir);
        try {
            new HttpEngineDownloadAction()
                    .setUrl(notation.toURL("https://proxy-maven.covers1624.net").toString())
                    .setDest(dest)
                    .setUseETag(true)
                    .setQuiet(false)
                    .setEngine(http)
                    .execute();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to download JavaSourceTransformer.", ex);
        }
        return dest;
    }
}
