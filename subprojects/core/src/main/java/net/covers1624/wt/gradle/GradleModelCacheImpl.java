/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.tconsole.ConsumingOutputStream;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.gradle.GradleModelCache;
import net.covers1624.wt.api.gradle.model.WorkspaceToolModel;
import net.covers1624.wt.event.ModuleHashCheckEvent;
import net.covers1624.wt.gradle.builder.SimpleBuildAction;
import net.covers1624.wt.util.HashContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.covers1624.quack.util.SneakyUtils.sneaky;
import static net.covers1624.wt.util.Utils.*;

/**
 * Created by covers1624 on 1/7/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class GradleModelCacheImpl implements GradleModelCache {

    private static final Logger LOGGER = LogManager.getLogger("GradleModelCache");
    private static final HashFunction sha256 = Hashing.sha256();
    private static final String HASH_MODULE_DATA = "wt:module-data";

    //hax
    private static final String cs_ihi = "org.gradle.tooling.internal.adapter.ProtocolToModelAdapter$InvocationHandlerImpl";
    private static final Class<?> c_ihi = sneaky(() -> Class.forName(cs_ihi));
    private static final Field f_sourceObject = sneaky(() -> makeAccessible(c_ihi.getDeclaredField("sourceObject")));

    private static final Set<String> hashedFiles = Sets.newHashSet(
            "build.gradle",
            "build.properties",
            "gradle.properties",
            "settings.gradle"
    );

    private final WorkspaceToolContext context;
    private final Path dataDir;

    public GradleModelCacheImpl(WorkspaceToolContext context) {
        this.context = context;
        dataDir = context.cacheDir.resolve("gradle_data");
        if (!Files.exists(dataDir)) {
            sneaky(() -> Files.createDirectories(dataDir));
        }
    }

    @Override
    public WorkspaceToolModel getModel(Path modulePath, Set<String> extraHash, Set<String> extraTasks) {
        String relPath = context.projectDir.relativize(modulePath).toString();
        LOGGER.info("Processing module: {}", relPath);
        HashContainer hashContainer = new HashContainer(dataDir.resolve(relPath.replace("/", "_") + "_cache.json"));

        //TODO, Event here to allow Forge module to add various other files.
        List<Path> toHash = StreamSupport.stream(Iterables.concat(hashedFiles, extraHash).spliterator(), true)
                .map(modulePath::resolve)
                .filter(Files::exists)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        if (toHash.isEmpty()) {
            throw new RuntimeException("Module without cacheable files? " + modulePath);
        }
        LOGGER.debug("Hashed files: {}", toHash.stream().map(Path::toString).collect(Collectors.joining(", ")));
        Path dataFile = dataDir.resolve(relPath.replace("\\", "/").replace("/", "_") + "_data.dat");

        Hasher moduleHasher = sha256.newHasher();
        toHash.forEach(e -> addToHasher(moduleHasher, e));
        HashMap<String, HashCode> hashes = new HashMap<>();
        hashes.put(HASH_MODULE_DATA, moduleHasher.hash());

        ModuleHashCheckEvent event = ModuleHashCheckEvent.REGISTRY.fireEvent(new ModuleHashCheckEvent(modulePath));
        hashes.putAll(event.getExtraHashes());
        Map<String, HashCode> outOfDate = hashes.entrySet().stream().filter(e -> hashContainer.check(e.getKey(), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        boolean isOutOfDate = !outOfDate.isEmpty();
        boolean isMissingCache = Files.notExists(dataFile);
        if (isOutOfDate) {
            LOGGER.debug("Out Of Date Hashes: " + String.join(", ", hashes.keySet()));
        }

        if (isOutOfDate || isMissingCache) {
            LOGGER.info("Update triggered, {}.", isMissingCache ? "missing cache" : "out-of-date");
            WorkspaceToolModel proxyModel = getModelFromGradle(modulePath, extraTasks);
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(IOUtils.makeParents(dataFile)))) {
                oos.writeObject(getNonProxyModel(proxyModel));
            } catch (IOException e) {
                throw new RuntimeException("Unable to write data file. " + dataFile, e);
            }
            outOfDate.forEach(hashContainer::set);
        } else {
            LOGGER.info("Up-to-date.");
        }

        //NOTE: Do Not hot wire this to return the result of GradleExecutor.getWorkspaceToolModel.
        //      The object returned by that method is behind a java Proxy class due to classloader
        //      stuff with gradle.
        //      Re-Reading the file on purpose after the Object has been unwrapped from its Proxy
        //      and written to file is the only workaround for this, at least the only one i can be
        //      bothered with, if someone has a better idea, besides re-writing all Data classes
        //      to be interfaces && Impl classes, go for it. Idc what it is, some form of ASM
        //      or different impl for Gradle Tooling would be acceptable.
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(dataFile))) {
            return (WorkspaceToolModel) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Unable to read data file. " + dataFile, e);
        }
    }

    private WorkspaceToolModel getModelFromGradle(Path modulePath, Set<String> extraTasks) {
        String gradleVersion = context.gradleManager.getGradleVersionForProject(modulePath);
        GradleConnector connector = GradleConnector.newConnector()
                .useGradleVersion(gradleVersion)
                .forProjectDirectory(modulePath.toFile());
        Path javaHome;
        try {
            javaHome = context.getJavaInstall(context.gradleManager.getJavaVersionForGradle(gradleVersion)).javaHome;
        } catch (IOException e) {
            throw new RuntimeException("Unable to get Java install.", e);
        }
        LOGGER.info("Using JDK {}.", javaHome);
        TailGroup tailGroup = context.console.newGroupFirst();
        try (ProjectConnection connection = connector.connect()) {
            LOGGER.info("Extracting project information..");
            GradleProject project = connection.model(GradleProject.class)
                    .setJavaHome(javaHome.toAbsolutePath().toFile())
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::error))
                    .addProgressListener(new GradleProgressListener(context, tailGroup))
                    .get();
            Set<String> executeBefore = new HashSet<>();
            executeBefore.addAll(context.gradleManager.getExecuteBefore());
            executeBefore.addAll(extraTasks);
            Set<String> availableTasks = project.getTasks().stream()
                    .map(Task::getName)
                    .collect(Collectors.toSet());
            LOGGER.debug("Available Tasks: {}", String.join(", ", availableTasks));
            Set<String> toExecute = executeBefore.stream()
                    .filter(availableTasks::contains)
                    .collect(Collectors.toSet());
            Set<String> notExecuting = executeBefore.stream()
                    .filter(e -> !toExecute.contains(e))
                    .collect(Collectors.toSet());
            if (!notExecuting.isEmpty()) {
                LOGGER.info("The following tasks will not be executed: {}", String.join(", ", notExecuting));
            }
            LOGGER.info("Extracting WorkspaceTool information.");
            WorkspaceToolModel run = connection
                    .action(new SimpleBuildAction<>(WorkspaceToolModel.class, context.gradleManager.getDataBuilders()))
                    .setJavaHome(javaHome.toAbsolutePath().toFile())
                    .setJvmArguments("-Xmx3G", "-Dorg.gradle.daemon=false")
                    .setEnvironmentVariables(Map.copyOf(System.getenv()))
                    .setStandardOutput(new ConsumingOutputStream(LOGGER::info))
                    .setStandardError(new ConsumingOutputStream(LOGGER::error))
                    .addProgressListener(new GradleProgressListener(context, tailGroup))
                    .withArguments("-si", "-I", context.gradleManager.getInitScript().toAbsolutePath().toString())
                    .forTasks(toExecute).run();

            return run;
        } finally {
            context.console.removeGroup(tailGroup);
        }
    }

    //Hax to retrieve the underlying object of a Proxied Model from Gradle Tooling.
    private static Object getNonProxyModel(WorkspaceToolModel model) {
        return getField(f_sourceObject, Proxy.getInvocationHandler(model));
    }

}
