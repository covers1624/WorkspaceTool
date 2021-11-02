/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import groovy.lang.Binding;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.nio.file.Path;

/**
 * Called when a WorkspaceScript is being prepared for execution.
 * Use this to provide any groovy imports or whatever.
 *
 * Created by covers1624 on 30/6/19.
 */
public class PrepareScriptEvent extends Event {

    public static final EventRegistry<PrepareScriptEvent> REGISTRY = new EventRegistry<>(PrepareScriptEvent.class);

    private final Binding binding;
    private final Path scriptFile;
    private final CompilerConfiguration compilerConfiguration;

    public PrepareScriptEvent(Binding binding, Path scriptFile, CompilerConfiguration compilerConfiguration) {
        this.binding = binding;
        this.scriptFile = scriptFile;
        this.compilerConfiguration = compilerConfiguration;
    }

    public CompilerConfiguration getCompilerConfiguration() {
        return compilerConfiguration;
    }

    public Path getScriptFile() {
        return scriptFile;
    }

    public Binding getBinding() {
        return binding;
    }
}
