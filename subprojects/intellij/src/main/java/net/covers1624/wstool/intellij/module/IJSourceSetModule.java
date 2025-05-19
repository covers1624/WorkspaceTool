package net.covers1624.wstool.intellij.module;

import net.covers1624.wstool.api.module.Dependency;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.SourceSet;

import java.nio.file.Path;
import java.util.*;

/**
 * Created by covers1624 on 5/5/25.
 */
public class IJSourceSetModule extends IJModule implements SourceSet {

    private final Module module;

    private final Map<String, List<Path>> sourcePaths = new LinkedHashMap<>();
    private final List<Dependency> compileDependencies = new ArrayList<>();
    private final List<Dependency> runtimeDependencies = new ArrayList<>();

    public IJSourceSetModule(ModulePath path, Module module) {
        super(path);
        this.module = module;
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public Map<String, List<Path>> sourcePaths() {
        return sourcePaths;
    }

    @Override
    public List<Dependency> compileDependencies() {
        return compileDependencies;
    }

    @Override
    public List<Dependency> runtimeDependencies() {
        return runtimeDependencies;
    }

    @Override
    public boolean isForTests() {
        return path.name().equals("test");
    }

    @Override
    public List<ContentPath> getContentPaths() {
        boolean forTests = isForTests();
        List<ContentPath> paths = new ArrayList<>(super.getContentPaths());
        sourcePaths.forEach((type, p) -> p.forEach(path -> {
            if (type.equals("resources")) {
                paths.add(ContentPath.resources(path, forTests));
            } else {
                paths.add(ContentPath.code(path, forTests));
            }
        }));

        return paths;
    }
}
