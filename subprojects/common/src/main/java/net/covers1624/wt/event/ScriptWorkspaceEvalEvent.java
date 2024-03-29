/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import net.covers1624.wt.api.mixin.MixinInstantiator;
import net.covers1624.wt.api.script.WorkspaceScript;

/**
 * Created by covers1624 on 8/8/19.
 */
public class ScriptWorkspaceEvalEvent extends Event {

    public static EventRegistry<ScriptWorkspaceEvalEvent> REGISTRY = new EventRegistry<>(ScriptWorkspaceEvalEvent.class);

    private final WorkspaceScript script;
    private final MixinInstantiator mixinInstantiator;

    public ScriptWorkspaceEvalEvent(WorkspaceScript script, MixinInstantiator mixinInstantiator) {
        this.script = script;
        this.mixinInstantiator = mixinInstantiator;
    }

    public MixinInstantiator getMixinInstantiator() {
        return mixinInstantiator;
    }

    public WorkspaceScript getScript() {
        return script;
    }
}
