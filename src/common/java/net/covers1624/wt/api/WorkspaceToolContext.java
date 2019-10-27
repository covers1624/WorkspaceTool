package net.covers1624.wt.api;

import com.google.common.collect.Iterables;
import net.covers1624.wt.api.dependency.DependencyLibrary;
import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.gradle.GradleModelCache;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.script.WorkspaceScript;
import net.covers1624.wt.api.tail.AnsiTailConsole;
import net.covers1624.wt.api.workspace.WorkspaceModule;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;
import net.covers1624.wt.util.TypedMap;
import net.covers1624.wt.util.scala.ScalaSdk;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The code flow of events and other handlers is laid out using comments in between the fields.
 *
 * Created by covers1624 on 11/8/19.
 */
public class WorkspaceToolContext {

    public TypedMap blackboard = new TypedMap();

    public Path projectDir;
    public Path cacheDir;
    public AnsiTailConsole console;

    //Extension load.

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

    public ScalaSdk scalaSdk;

    //ProcessDependencyEvent

    //ProcessModulesEvent

    //WorkspaceHandler.buildWorkspaceModules

    public List<WorkspaceModule> workspaceModules = new ArrayList<>();

    //ProcessWorkspaceModulesEvent

    public Iterable<Module> getAllModules() {
        return Iterables.concat(frameworkModules, modules);
    }
}
