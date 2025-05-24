package net.covers1624.wstool.intellij.workspace;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by covers1624 on 5/5/25.
 */
public class IJProjectModule extends IJModuleWithPath implements Module {

    private final IJWorkspace workspace;
    private final Map<ModulePath, IJProjectModule> subModules = new LinkedHashMap<>();
    private final Map<ModulePath, IJSourceSetModule> sourceSets = new LinkedHashMap<>();

    private @Nullable ProjectData gradleData;

    public IJProjectModule(IJWorkspace workspace, Path rootDir, ModulePath path) {
        super(rootDir, path);
        this.workspace = workspace;
    }

    @Override
    public Map<String, ? extends Module> subModules() {
        return FastStream.of(subModules.entrySet())
                .toMap(e -> e.getKey().name(), Map.Entry::getValue);
    }

    @Override
    public Map<String, ? extends SourceSet> sourceSets() {
        return FastStream.of(sourceSets.entrySet())
                .toMap(e -> e.getKey().name(), Map.Entry::getValue);
    }

    @Override
    public Module newSubModule(Path rootDir, String name) {
        ModulePath path = this.path.with(name);

        IJProjectModule module = new IJProjectModule(workspace, rootDir, path);
        workspace.addModule(module);
        subModules.put(path, module);
        return module;
    }

    @Override
    public SourceSet newSourceSet(String name) {
        ModulePath path = this.path.with(name);

        IJSourceSetModule module = new IJSourceSetModule(path, this);
        workspace.addModule(module);
        sourceSets.put(path, module);
        return module;
    }

    @Override
    public @Nullable ProjectData projectData() {
        return gradleData;
    }

    @Override
    public void setProjectData(ProjectData data) {
        gradleData = data;
    }
}
