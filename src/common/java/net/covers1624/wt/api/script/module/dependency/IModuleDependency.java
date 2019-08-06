package net.covers1624.wt.api.script.module.dependency;

import java.util.List;

/**
 * Created by covers1624 on 29/05/19.
 */
public interface IModuleDependency extends IDependency {

    String getGroup();

    String getModuleName();

    List<String> getSourceSets();
}
