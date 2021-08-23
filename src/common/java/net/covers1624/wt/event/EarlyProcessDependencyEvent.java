package net.covers1624.wt.event;

import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;

/**
 * Called to process the dependency on a Module's Configuration.
 *
 * The Forge extension uses this for DeobfCompile and DeobfProvided handling.
 *
 * Created by covers1624 on 26/7/19.
 */
public class EarlyProcessDependencyEvent extends ResultEvent<Dependency> {

    public static final EventRegistry<EarlyProcessDependencyEvent> REGISTRY = new EventRegistry<>(EarlyProcessDependencyEvent.class);

    private final WorkspaceToolContext context;
    private final Module module;
    private final Configuration sourceSetConfig;
    private final Configuration dependencyConfig;
    private final Dependency dependency;

    public EarlyProcessDependencyEvent(WorkspaceToolContext context, Module module, Configuration sourceSetConfig, Configuration dependencyConfig, Dependency dependency) {
        super(false);
        this.context = context;
        this.module = module;
        this.sourceSetConfig = sourceSetConfig;
        this.dependencyConfig = dependencyConfig;
        this.dependency = dependency;
    }

    @Override
    public Dependency getResult() {
        if (!hasResult()) {
            return dependency;
        }
        return super.getResult();
    }

    public WorkspaceToolContext getContext() {
        return context;
    }

    public Module getModule() {
        return module;
    }

    public Configuration getSourceSetConfig() {
        return sourceSetConfig;
    }

    public Configuration getDependencyConfig() {
        return dependencyConfig;
    }

    public Dependency getDependency() {
        return dependency;
    }
}
