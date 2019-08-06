package net.covers1624.wt.event;

import net.covers1624.wt.api.framework.FrameworkRegistry;
import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.api.workspace.WorkspaceRegistry;

/**
 * Called at the beginning of WorkspaceTool's LifeCycle to add any extra functionality to
 * WorkspaceTool subsystems.
 *
 * Created by covers1624 on 30/6/19.
 */
public class InitializationEvent extends Event {

    public static final EventRegistry<InitializationEvent> REGISTRY = new EventRegistry<>(InitializationEvent.class);

    private final FrameworkRegistry frameworkRegistry;
    private final GradleManager gradleManager;
    private final WorkspaceRegistry workspaceRegistry;

    public InitializationEvent(FrameworkRegistry frameworkRegistry, GradleManager gradleManager, WorkspaceRegistry workspaceRegistry) {
        this.frameworkRegistry = frameworkRegistry;
        this.gradleManager = gradleManager;
        this.workspaceRegistry = workspaceRegistry;
    }

    public GradleManager getGradleManager() {
        return gradleManager;
    }

    public FrameworkRegistry getFrameworkRegistry() {
        return frameworkRegistry;
    }

    public WorkspaceRegistry getWorkspaceRegistry() {
        return workspaceRegistry;
    }
}
