package net.covers1624.wt.api.impl.script;

import groovy.lang.Binding;
import groovy.lang.Script;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.framework.ModdingFramework;
import net.covers1624.wt.api.impl.script.module.ModuleContainerImpl;
import net.covers1624.wt.api.impl.script.runconfig.DefaultRunConfigContainer;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.script.runconfig.RunConfigContainer;
import net.covers1624.wt.api.workspace.Workspace;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 13/05/19.
 */
public abstract class AbstractWorkspaceScript extends Script implements WorkspaceScript {

    public static final String FR_PROP = "frameworkRegistry";
    public static final String WR_PROP = "workspaceRegistry";

    private boolean firstPass;
    private DefaultScriptDepsContainer scriptDeps = new DefaultScriptDepsContainer();
    private FrameworkRegistry frameworkRegistry;
    private WorkspaceRegistry workspaceRegistry;
    private Class<? extends ModdingFramework> frameworkClass;
    private Class<? extends Workspace> workspaceType;
    private ModdingFramework framework;
    private Workspace workspace;
    private ModuleContainerImpl workspaceModules;
    private Map<String, String> depOverrides = new HashMap<>();
    private RunConfigContainer runConfigContainer = new DefaultRunConfigContainer();

    public AbstractWorkspaceScript() {
        super();
    }

    public AbstractWorkspaceScript(Binding binding) {
        super(binding);
    }

    @Override
    public void setBinding(Binding binding) {
        super.setBinding(binding);
        firstPass = getProp("firstPass");
        if (!firstPass) {
            frameworkRegistry = getProp(FR_PROP);
            workspaceRegistry = getProp(WR_PROP);
        }
    }

    @Override
    public Path path(String str) {
        return Paths.get(str);
    }

    //    @Override
    //    public void scriptDeps(Consumer<ScriptDepsSpec> consumer) {
    //        consumer.accept(scriptDeps);
    //        if (firstPass) {
    //            throw new AbortScriptException();
    //        }
    //    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends ModdingFramework> void framework(Class<T> clazz, Consumer<T> consumer) {
        if (frameworkClass != null && (!frameworkClass.equals(clazz))) {
            throw new IllegalArgumentException("Multi framework is currently not supported.");
        }
        if (frameworkClass == null) {
            frameworkClass = clazz;
            framework = frameworkRegistry.constructScriptImpl(frameworkClass);
        }
        consumer.accept((T) framework);
    }

    @Override
    public void depOverride(Map<String, String> overrides) {
        depOverrides.putAll(overrides);
    }

    @Override
    public void depOverride(String from, String to) {
        depOverrides.put(from, to);
    }

    @Override
    public void modules(Consumer<ModuleContainerSpec> consumer) {
        if (workspaceModules == null) {
            workspaceModules = new ModuleContainerImpl();
        }
        consumer.accept(workspaceModules);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> void workspace(Class<T> clazz, Consumer<T> consumer) {
        if (workspaceType != null && (!workspaceType.equals(clazz))) {
            throw new IllegalArgumentException("Multi workspace output is currently not supported.");
        }
        if (workspaceType == null) {
            workspaceType = clazz;
            workspace = workspaceRegistry.constructScriptImpl(workspaceType);
        }
        consumer.accept((T) workspace);
    }

    @Override
    public <T extends Workspace> void workspace(Class<T> clazz) {
        workspace(clazz, e -> {
        });
    }

    @Override
    public void runConfigs(Consumer<RunConfigContainer> consumer) {
        consumer.accept(runConfigContainer);
    }

    public List<String> getRepos() {
        return scriptDeps.getRepos();
    }

    public List<String> getClasspathDeps() {
        return scriptDeps.getClasspathDeps();
    }

    public Class<? extends ModdingFramework> getFrameworkClass() {
        return frameworkClass;
    }

    public ModdingFramework getFramework() {
        return framework;
    }

    public Class<? extends Workspace> getWorkspaceType() {
        return workspaceType;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Map<String, String> getDepOverrides() {
        return depOverrides;
    }

    public ModuleContainerImpl getWorkspaceModules() {
        return workspaceModules;
    }

    @Override
    public RunConfigContainer getRunConfigContainer() {
        return runConfigContainer;
    }

    @SuppressWarnings ("unchecked")
    private <T> T getProp(String name) {
        return (T) getProperty(name);
    }

    public static class AbortScriptException extends RuntimeException {

    }

}
