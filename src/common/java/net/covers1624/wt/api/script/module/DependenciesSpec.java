package net.covers1624.wt.api.script.module;

import net.covers1624.wt.api.script.module.dependency.IDependency;
import net.covers1624.wt.api.script.module.dependency.IMavenDependency;
import net.covers1624.wt.api.script.module.dependency.IModuleDependency;

/**
 * Created by covers1624 on 27/05/19.
 */
public interface DependenciesSpec {

    void compile(IDependency obj);

    void provided(IDependency obj);

    void runtime(IDependency obj);

    void test(IDependency obj);

    IModuleDependency module(String str);

    IMavenDependency maven(String str);
}
