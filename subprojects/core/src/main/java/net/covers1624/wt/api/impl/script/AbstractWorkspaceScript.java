/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script;

import groovy.lang.Binding;
import groovy.lang.Script;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.impl.script.module.ModuleContainerImpl;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.ModdingFramework;
import net.covers1624.wt.api.script.NullFramework;
import net.covers1624.wt.api.script.Workspace;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.script.module.ModuleContainerSpec;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import net.covers1624.wt.event.ScriptWorkspaceEvalEvent;
import net.covers1624.wt.util.JavaVersion;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 13/05/19.
 */
public abstract class AbstractWorkspaceScript extends Script implements WorkspaceScript {

    public static final String FR_PROP = "frameworkRegistry";
    public static final String WR_PROP = "workspaceRegistry";
    public static final String MI_PROP = "mixinInstantiator";

    private FrameworkRegistry frameworkRegistry;
    private WorkspaceRegistry workspaceRegistry;
    private MixinInstantiator mixinInstantiator;
    private Class<? extends ModdingFramework> frameworkClass;
    private Class<? extends Workspace> workspaceType;
    private ModdingFramework framework;
    private Workspace workspace;
    private ModuleContainerSpec moduleContainer;
    private final Map<String, String> depOverrides = new HashMap<>();
    private JavaVersion javaVersion = JavaVersion.JAVA_8;

    public AbstractWorkspaceScript() {
        super();
    }

    public AbstractWorkspaceScript(Binding binding) {
        super(binding);
    }

    @Override
    public void setBinding(Binding binding) {
        super.setBinding(binding);
        frameworkRegistry = getProp(FR_PROP);
        workspaceRegistry = getProp(WR_PROP);
        mixinInstantiator = getProp(MI_PROP);
    }

    @Override
    public Path path(String str) {
        return Paths.get(str);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends ModdingFramework> void framework(Class<T> clazz, Consumer<T> consumer) {
        if (frameworkClass != null && (!frameworkClass.equals(clazz))) {
            throw new IllegalArgumentException("Multi framework is currently not supported.");
        }
        if (frameworkClass == null) {
            frameworkClass = clazz != null ? clazz : NullFramework.class;
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
    public void setJdk(JavaVersion javaVersion) {
        this.javaVersion = javaVersion;
    }

    @Override
    public void modules(Consumer<ModuleContainerSpec> consumer) {
        if (moduleContainer == null) {
            moduleContainer = new ModuleContainerImpl();
        }
        consumer.accept(moduleContainer);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T extends Workspace> void workspace(Class<T> clazz, Consumer<T> consumer) {
        if (workspaceType != null && (!workspaceType.equals(clazz))) {
            throw new IllegalArgumentException("Multi workspace output is currently not supported.");
        }
        if (workspaceType == null) {
            workspaceType = clazz;
            workspace = workspaceRegistry.constructScriptImpl(workspaceType, mixinInstantiator);
        }
        ScriptWorkspaceEvalEvent.REGISTRY.fireEvent(new ScriptWorkspaceEvalEvent(this, mixinInstantiator));
        consumer.accept((T) workspace);
    }

    @Override
    public <T extends Workspace> void workspace(Class<T> clazz) {
        workspace(clazz, e -> {
        });
    }

    @Override
    public Class<? extends ModdingFramework> getFrameworkClass() {
        return frameworkClass;
    }

    @Override
    public ModdingFramework getFramework() {
        return framework;
    }

    @Override
    public Class<? extends Workspace> getWorkspaceType() {
        return workspaceType;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public Map<String, String> getDepOverrides() {
        return depOverrides;
    }

    @Override
    public JavaVersion getJdk() {
        return javaVersion;
    }

    @Override
    public ModuleContainerSpec getModuleContainer() {
        return moduleContainer;
    }

    @SuppressWarnings ("unchecked")
    private <T> T getProp(String name) {
        return (T) getProperty(name);
    }
}
