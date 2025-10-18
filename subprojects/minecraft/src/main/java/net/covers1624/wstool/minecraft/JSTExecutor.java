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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public void applyJST(SourceSet classpath, Path mcSources, List<Path> ifaceInjections, List<Path> accessTransformers) {
        if (ifaceInjections.isEmpty() && accessTransformers.isEmpty()) return;

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

        // Input and output are identical.
        args.add(mcSources.toAbsolutePath().toString());
        args.add(mcSources.toAbsolutePath().toString());

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
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Failed to run JST.", ex);
        } finally {
            try {
                Files.deleteIfExists(librariesList);
            } catch (IOException ignored) {
            }
        }
    }

    private static Path createLibrariesList(SourceSet sourceSet, Path mcSources) {
        var classpath = collectClasspath(sourceSet);
        classpath.remove(mcSources);
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

    private Path getJstBundle() {
        if (jstBundlePath == null) {
            jstBundlePath = downloadJst();
        }
        return jstBundlePath;
    }

    private Path downloadJst() {
        Path dest = JST_NOTATION.toPath(librariesDir);
        try {
            new HttpEngineDownloadAction()
                    .setUrl(JST_NOTATION.toURL("https://proxy-maven.covers1624.net").toString())
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
