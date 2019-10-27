package net.covers1624.wt.intellij.api.impl


import net.covers1624.wt.api.impl.workspace.AbstractWorkspace
import net.covers1624.wt.api.mixin.MixinInstantiator
import net.covers1624.wt.intellij.api.script.Intellij

/**
 * Created by covers1624 on 23/7/19.
 */
class IntellijImpl extends AbstractWorkspace implements Intellij {

    private boolean modulePerSourceSet

    IntellijImpl(MixinInstantiator mixinInstantiator) {
        super(mixinInstantiator)
    }

    @Override
    void setModulePerSourceSet(boolean bool) {
        modulePerSourceSet = bool
    }

    @Override
    boolean getModulePerSourceSet() {
        return modulePerSourceSet
    }
}
