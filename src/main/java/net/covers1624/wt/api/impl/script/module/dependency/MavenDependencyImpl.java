package net.covers1624.wt.api.impl.script.module.dependency;

import net.covers1624.wt.api.script.module.dependency.IMavenDependency;
import net.covers1624.wt.util.MavenNotation;

/**
 * Created by covers1624 on 29/05/19.
 */
public class MavenDependencyImpl implements IMavenDependency {

    private final MavenNotation notation;

    public MavenDependencyImpl(MavenNotation notation) {
        this.notation = notation;
    }

    public static MavenDependencyImpl parse(String from) {
        return new MavenDependencyImpl(MavenNotation.parse(from));
    }

    @Override
    public MavenNotation getNotation() {
        return notation;
    }
}
