package net.covers1624.wt.event;

import net.covers1624.wt.api.dependency.Dependency;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import net.covers1624.wt.api.module.ModuleList;

import java.nio.file.Path;

/**
 * Called to process the dependency on a Module's Configuration.
 *
 * The Forge extension uses this for DeobfCompile and DeobfProvided handling.
 *
 * Created by covers1624 on 26/7/19.
 */
public class ProcessDependencyEvent extends ResultEvent<Dependency> {

    public static final EventRegistry<ProcessDependencyEvent> REGISTRY = new EventRegistry<>(ProcessDependencyEvent.class);

    private final Path cacheDir;
    private final Module module;
    private final Configuration sourceSetConfig;
    private final Configuration dependencyConfig;
    private final Dependency dependency;
    private final ModuleList modules;

    public ProcessDependencyEvent(Path cacheDir, Module module, Configuration sourceSetConfig, Configuration dependencyConfig, Dependency dependency, ModuleList modules) {
        super(false);
        this.cacheDir = cacheDir;
        this.module = module;
        this.sourceSetConfig = sourceSetConfig;
        this.dependencyConfig = dependencyConfig;
        this.dependency = dependency;
        this.modules = modules;
    }

    @Override
    public Dependency getResult() {
        if (!hasResult()) {
            return dependency;
        }
        return super.getResult();
    }

    public Dependency getDependency() {
        return dependency;
    }

    public Configuration getDependencyConfig() {
        return dependencyConfig;
    }

    public Module getModule() {
        return module;
    }

    public ModuleList getModuleList() {
        return modules;
    }

    public Configuration getSourceSetConfig() {
        return sourceSetConfig;
    }

    public Path getCacheDir() {
        return cacheDir;
    }
}
