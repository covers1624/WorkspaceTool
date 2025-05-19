package net.covers1624.wstool.intellij.module;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.SourceSet;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by covers1624 on 5/5/25.
 */
public class IJProjectModule extends IJModuleWithPath implements Module {

    private final IJWorkspaceBuilder container;
    private final Map<ModulePath, IJProjectModule> subModules = new LinkedHashMap<>();
    private final Map<ModulePath, IJSourceSetModule> sourceSets = new LinkedHashMap<>();

    public IJProjectModule(IJWorkspaceBuilder container, Path rootDir, ModulePath path) {
        super(rootDir, path);
        this.container = container;
    }

    @Override
    public Map<String, ? extends Module> subModules() {
        return FastStream.of(subModules.entrySet())
                .toMap(e-> e.getKey().name(), Map.Entry::getValue);
    }

    @Override
    public Map<String, ? extends SourceSet> sourceSets() {
        return FastStream.of(sourceSets.entrySet())
                .toMap(e-> e.getKey().name(), Map.Entry::getValue);
    }

    @Override
    public Module newSubModule(Path rootDir, String name) {
        ModulePath path = this.path.with(name);

        IJProjectModule module = new IJProjectModule(container, rootDir, path);
        container.addModule(module);
        subModules.put(path, module);
        return module;
    }

    @Override
    public SourceSet newSourceSet(String name) {
        ModulePath path = this.path.with(name);

        IJSourceSetModule module = new IJSourceSetModule(path, this);
        container.addModule(module);
        sourceSets.put(path, module);
        return module;
    }
}
