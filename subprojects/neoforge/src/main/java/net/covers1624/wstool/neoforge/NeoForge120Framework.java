package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.extension.Framework;
import net.covers1624.wstool.api.module.Module;
import net.covers1624.wstool.api.module.WorkspaceBuilder;
import net.covers1624.wstool.gradle.api.data.ProjectData;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge120Framework() implements Framework {

    @Override
    public void buildFrameworks(Environment env, BiFunction<Path, Set<String>, ProjectData> dataExtractor, BiFunction<WorkspaceBuilder, ProjectData, Module> moduleFactory, WorkspaceBuilder workspaceBuilder) {

    }
}
