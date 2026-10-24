package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.api.workspace.runs.EvalList;
import net.covers1624.wstool.api.workspace.runs.EvalMap;
import net.covers1624.wstool.api.workspace.runs.RunConfig;
import net.covers1624.wstool.api.workspace.DependencyResolver;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 5/25/25.
 */
public final class IJRunConfig implements RunConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(IJRunConfig.class);

    private final String name;

    private final Map<String, String> config = new LinkedHashMap<>();

    private @Nullable Path runDir;
    private @Nullable SourceSet classpath;
    private @Nullable String mainClass;

    private final EvalList args = new EvalList();
    private final EvalList vmArgs = new EvalList();
    private final EvalMap sysProps = new EvalMap();
    private final EvalMap envVars = new EvalMap();

    public IJRunConfig(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, String> config() {
        return config;
    }

    @Override
    public @Nullable Path runDir() {
        return runDir;
    }

    @Override
    public void runDir(Path runDir) {
        this.runDir = runDir;
    }

    @Override
    public @Nullable SourceSet classpath() {
        return classpath;
    }

    @Override
    public void classpath(SourceSet classpath) {
        this.classpath = classpath;
    }

    @Override
    public @Nullable String mainClass() {
        return mainClass;
    }

    @Override
    public void mainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public EvalList args() {
        return args;
    }

    @Override
    public EvalList vmArgs() {
        return vmArgs;
    }

    @Override
    public EvalMap sysProps() {
        return sysProps;
    }

    @Override
    public EvalMap envVars() {
        return envVars;
    }

    public Document writeDocument(Environment env, DependencyResolver depResolver) {
        Element component = new Element("component")
                .setAttribute("name", "ProjectRunConfigurationManager");

        String name;
        String group = null;
        int slashIdx = this.name.indexOf('/');
        if (slashIdx != -1) {
            name = this.name.substring(slashIdx + 1);
            group = this.name.substring(0, slashIdx);
        } else {
            name = this.name;
        }

        Element configuration = new Element("configuration")
                .setAttribute("default", "false")
                .setAttribute("name", name)
                .setAttribute("type", "Application")
                .setAttribute("factoryName", "Application");

        if (group != null) {
            configuration.setAttribute("folderName", group);
        }

        configuration.addContent(new Element("option")
                .setAttribute("name", "MAIN_CLASS_NAME")
                .setAttribute("value", requireNonNull(mainClass, "Run config " + name + " must have a main class."))
        );

        configuration.addContent(new Element("module")
                .setAttribute("name", ((IJSourceSetModule) requireNonNull(classpath, "Run config " + name + " must have a classpath.")).path.toString())
        );

        configuration.addContent(new Element("option")
                .setAttribute("name", "PROGRAM_PARAMETERS")
                .setAttribute("value", FastStream.of(args.toList(depResolver)).map(IJRunConfig::escape).join(" "))
        );

        List<String> vmArgs = new ArrayList<>(this.vmArgs.toList(depResolver));
        sysProps.toMap(depResolver).forEach((k, v) -> vmArgs.add("-D" + k + "=" + v));
        configuration.addContent(new Element("option")
                .setAttribute("name", "VM_PARAMETERS")
                .setAttribute("value", FastStream.of(vmArgs).map(IJRunConfig::escape).join(" "))
        );

        configuration.addContent(new Element("option")
                .setAttribute("name", "WORKING_DIRECTORY")
                .setAttribute("value", requireNonNull(runDir, "Run config " + name + " must have a run directory.").toAbsolutePath().normalize().toString())
        );

        if (!envVars.isEmpty()) {
            Element envs = new Element("envs");
            envVars.toMap(depResolver).forEach((k, v) -> {
                envs.addContent(new Element("env")
                        .setAttribute("name", k)
                        .setAttribute("value", v)
                );
            });
            configuration.addContent(envs);
        }

        // TODO classpathShortening toggle?

        Element method = new Element("method")
                .setAttribute("v", "2");
        method.addContent(new Element("option")
                .setAttribute("name", "MakeProject")
                .setAttribute("enabled", "true")
        );
        configuration.addContent(method);

        component.addContent(configuration);
        return new Document(component);
    }

    public void createWorkingDir() {
        if (runDir == null) return;

        try {
            Files.createDirectories(runDir);
        } catch (IOException ex) {
            LOGGER.warn("Unable to create working directory {} for run config {}.", runDir, name);
        }
    }

    private static String escape(String str) {
        if (str.contains(" ")) {
            return "\"" + str + "\"";
        }
        return str;
    }
}
