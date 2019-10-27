package net.covers1624.wt.api.impl.workspace;

import net.covers1624.wt.api.impl.script.runconfig.DefaultRunConfigContainer;
import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.Workspace;
import net.covers1624.wt.api.script.runconfig.RunConfigContainer;

import java.util.function.Consumer;

/**
 * Created by covers1624 on 8/8/19.
 */
public class AbstractWorkspace implements Workspace {

    private final RunConfigContainer runConfigContainer;

    public AbstractWorkspace(MixinInstantiator mixinInstantiator) {
        runConfigContainer = new DefaultRunConfigContainer(mixinInstantiator);
    }

    @Override
    public void runConfigs(Consumer<RunConfigContainer> consumer) {
        consumer.accept(runConfigContainer);
    }

    @Override
    public RunConfigContainer getRunConfigContainer() {
        return runConfigContainer;
    }
}
