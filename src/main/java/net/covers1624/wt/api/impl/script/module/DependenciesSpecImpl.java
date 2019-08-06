package net.covers1624.wt.api.impl.script.module;

import net.covers1624.wt.api.dependency.DependencyScope;
import net.covers1624.wt.api.impl.script.module.dependency.MavenDependencyImpl;
import net.covers1624.wt.api.impl.script.module.dependency.ModuleDependencyImpl;
import net.covers1624.wt.api.script.module.DependenciesSpec;
import net.covers1624.wt.api.script.module.dependency.IDependency;
import net.covers1624.wt.api.script.module.dependency.IMavenDependency;
import net.covers1624.wt.api.script.module.dependency.IModuleDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 27/05/19.
 */
public class DependenciesSpecImpl implements DependenciesSpec {

    private Map<DependencyScope, List<Object>> dependencyMap = new HashMap<>();

    @Override
    public void compile(IDependency obj) {
        dependencyMap.computeIfAbsent(DependencyScope.COMPILE, e -> new ArrayList<>()).add(obj);
    }

    @Override
    public void provided(IDependency obj) {
        dependencyMap.computeIfAbsent(DependencyScope.PROVIDED, e -> new ArrayList<>()).add(obj);
    }

    @Override
    public void runtime(IDependency obj) {
        dependencyMap.computeIfAbsent(DependencyScope.RUNTIME, e -> new ArrayList<>()).add(obj);
    }

    @Override
    public void test(IDependency obj) {
        dependencyMap.computeIfAbsent(DependencyScope.TEST, e -> new ArrayList<>()).add(obj);
    }

    @Override
    public IModuleDependency module(String str) {
        return ModuleDependencyImpl.parse(str);
    }

    @Override
    public IMavenDependency maven(String str) {
        return MavenDependencyImpl.parse(str);
    }
}
