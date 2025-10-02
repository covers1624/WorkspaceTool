package net.covers1624.wstool.intellij.workspace;

import net.covers1624.wstool.api.workspace.Dependency;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.SourceSet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 5/5/25.
 */
public final class IJSourceSetModule extends IJModule implements SourceSet {

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

    @Override
    public List<DependencyEntry> getDependencyEntries() {
        List<DependencyEntry> entries = new ArrayList<>();
        compileDependencies.forEach(dep -> entries.add(collectDependency(dep, DependencyScope.COMPILE)));
        runtimeDependencies.forEach(dep -> entries.add(collectDependency(dep, DependencyScope.RUNTIME)));
        return entries;
    }

    // TODO, we don't actually want to always export everything. We should extract more Gradle data about what dependencies
    //       are publicly exported (api, vs implementation) and use that.
    private DependencyEntry collectDependency(Dependency dep, DependencyScope scope) {
        return switch (dep) {
            case Dependency.MavenDependency mavenDep -> new MavenDependencyEntry(mavenDep, scope, true);
            case Dependency.SourceSetDependency(SourceSet sourceSet) -> new ProjectDependencyEntry(((IJModule) sourceSet), scope, true);
        };
    }
}
