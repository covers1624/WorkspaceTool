package net.covers1624.wstool.neoforge;

import net.covers1624.wstool.api.Environment;
import net.covers1624.wstool.api.ModuleProcessor;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.workspace.Module;
import net.covers1624.wstool.api.workspace.Workspace;
import net.covers1624.wstool.gradle.api.data.ProjectData;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Created by covers1624 on 21/10/24.
 */
public record NeoForge120FrameworkType() implements FrameworkType {

    @Override
    public void buildFrameworks(Environment env, ModuleProcessor moduleProcessor, Workspace workspace) {

    }
}
