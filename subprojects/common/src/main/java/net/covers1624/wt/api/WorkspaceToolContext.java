/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api;

import com.google.common.collect.Iterables;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaLocator;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.quack.collection.TypedMap;
import net.covers1624.quack.net.download.DownloadProgressTail;
import net.covers1624.tconsole.api.TailConsole;
import net.covers1624.tconsole.api.TailGroup;
import net.covers1624.tconsole.tails.TextTail;
import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.GradleModelCache;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.script.ModdingFramework;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.workspace.WorkspaceModule;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The code flow of events and other handlers is laid out using comments in between the fields.
 * <p>
 * Created by covers1624 on 11/8/19.
 */
public class WorkspaceToolContext {

    private static final Logger LOGGER = LogManager.getLogger();

    public TypedMap blackboard = new TypedMap();

    public Path projectDir;
    public Path cacheDir;
    public TailConsole console;

    //Extension load.

    public List<JavaInstall> javaInstalls;
    public JdkInstallationManager jdkManager;
    public FrameworkRegistry frameworkRegistry;
    public GradleManager gradleManager;
    public GradleModelCache modelCache;
    public WorkspaceRegistry workspaceRegistry;

    //InitializationEvent

    public MixinInstantiator mixinInstantiator;

    //PrepareScriptEvent

    public WorkspaceScript workspaceScript;
    public DependencyLibrary dependencyLibrary;
    public List<Module> modules = new ArrayList<>();
    public List<Module> frameworkModules = new ArrayList<>();

    //FrameworkHandler.constructFrameworkModules

    //ProcessDependencyEvent

    //ProcessModulesEvent

    //WorkspaceHandler.buildWorkspaceModules

    public List<WorkspaceModule> workspaceModules = new ArrayList<>();

    //ProcessWorkspaceModulesEvent

    public Iterable<Module> getAllModules() {
        return Iterables.concat(frameworkModules, modules);
    }

    public boolean isFramework(Class<? extends ModdingFramework> framework) {
        return framework.equals(workspaceScript.getFrameworkClass());
    }

    public JavaInstall getJavaInstall(JavaVersion version) throws IOException {
        for (JavaInstall javaInstall : javaInstalls) {
            if (javaInstall.langVersion == version) return javaInstall;
        }
        Path javaHome = jdkManager.findJdk(version, false);
        if (javaHome == null) {

            LOGGER.info("Unable to find compatible {} JDK. Provisioning..", version);

            TailGroup tailGroup = console.newGroupFirst();
            tailGroup.add(new TextTail(1))
                    .setLine(0, "===============================");
            DownloadProgressTail progress = new DownloadProgressTail();
            tailGroup.add(progress);
            javaHome = jdkManager.provisionJdk(version, false, progress);
            console.removeGroup(tailGroup);
        }
        return JavaLocator.parseInstall(JavaInstall.getJavaExecutable(javaHome, false));
    }
}
