package net.covers1624.wt.api.script.module.dependency;

import net.covers1624.wt.util.MavenNotation;

/**
 * Created by covers1624 on 29/05/19.
 */
public interface IMavenDependency extends IDependency {

    MavenNotation getNotation();

}
