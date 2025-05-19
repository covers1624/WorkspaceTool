package net.covers1624.wstool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.config.Config;
import net.covers1624.wstool.api.extension.Extension;
import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.extension.Workspace;
import net.covers1624.wstool.gradle.GradleModelExtractor;
import net.covers1624.wstool.gradle.api.data.*;
import net.covers1624.wstool.json.TypeFieldDeserializer;
import net.covers1624.wstool.module.ModuleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by covers1624 on 19/9/24.
 */
public class WorkspaceTool {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VERSION;

    static {
        String v = WorkspaceTool.class.getPackage().getImplementationVersion();
        if (v == null) {
            v = "dev";
        }
        VERSION = v;
    }

    public static void main(String[] args) throws IOException {
        Environment env = Environment.of();
        LOGGER.info("Starting WorkspaceTool v@{}", VERSION);
        LOGGER.info("  Project Directory: {}", env.projectRoot());
        List<Extension> extensions = loadExtensions();

        Path configFile = env.projectRoot().resolve("workspace.yml");
        if (Files.notExists(configFile)) {
            configFile = env.projectRoot().resolve("workspace.yaml");
        }
        if (Files.notExists(configFile)) {
            LOGGER.error("Expected workspace.yml or workspace.yaml in project directory.");
            return;
        }

        Config config = deserializeConfig(extensions, configFile);
        Workspace workspace = config.workspace();

        // TODO, currently only one framework supported.
        //       Ideally, Fabric will be a framework.
        if (config.frameworks().size() != 1) {
            throw new RuntimeException("Expected one framework, got: " + config.frameworks().size());
        }
        Framework framework = config.frameworks().get(0);

        List<Path> modulePaths = FastStream.of(config.modules())
                .flatMap(e -> {
                    try {
                        return ModuleUtils.expandModuleReference(env.projectRoot(), e);
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to expand module reference.", ex);
                    }
                })
                .filterNot(e -> e.getFileName().toString().equals("buildSrc"))
                .filter(e -> Files.exists(e.resolve("build.gradle")))
                .toList();
        LOGGER.info("Found {} modules.", modulePaths.size());

        JdkProvider jdkProvider = new JdkProvider(env);
        GradleModelExtractor modelExtractor = new GradleModelExtractor(env, jdkProvider, config.gradleHashables());

        for (Path modulePath : modulePaths) {
            LOGGER.info("Processing module {}", env.projectRoot().relativize(modulePath));
            ProjectData projectData = modelExtractor.extractProjectData(modulePath, Set.of());
        }
    }

    private static List<Extension> loadExtensions() {
        LOGGER.info("Loading extensions...");
        List<Extension> extensions = new ArrayList<>();
        for (Extension extension : ServiceLoader.load(Extension.class)) {
            Extension.Details details = extension.getClass().getAnnotation(Extension.Details.class);
            if (details == null) {
                throw new RuntimeException("Extension " + extension.getClass().getName() + " requires @Extension.Details annotation.");
            }
            extensions.add(extension);
            LOGGER.info("  Loaded extension {}, {}", details.id(), details.desc());
        }
        LOGGER.info("Loaded {} extensions.", extensions.size());
        return extensions;
    }

    private static Config deserializeConfig(List<Extension> extensions, Path config) throws IOException {
        Gson gson = createDeserializer(extensions).create();
        try (Reader reader = new InputStreamReader(Files.newInputStream(config))) {
            return gson.fromJson(gson.toJson((Object) new Yaml().load(reader)), Config.class);
        }
    }

    private static @NotNull GsonBuilder createDeserializer(List<Extension> extensions) {
        GsonBuilder builder = new GsonBuilder();

        Map<String, Class<? extends Framework>> frameworkTypes = new HashMap<>();
        Map<String, Class<? extends Workspace>> workspaceTypes = new HashMap<>();
        builder.registerTypeAdapter(Framework.class, new TypeFieldDeserializer<>("framework", frameworkTypes));
        builder.registerTypeAdapter(Workspace.class, new TypeFieldDeserializer<>("workspace", workspaceTypes));

        Extension.ConfigTypeRegistry registry = new Extension.ConfigTypeRegistry() {

            @Override
            public void registerFramework(String key, Class<? extends Framework> type) {
                if (frameworkTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }

            @Override
            public void registerWorkspace(String key, Class<? extends Workspace> type) {
                if (workspaceTypes.put(key, type) != null) throw new IllegalArgumentException("Type " + key + " already exists.");
            }
        };

        for (Extension extension : extensions) {
            extension.registerConfigTypes(registry);
        }
        return builder;
    }
}
