/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle;

import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.gradle.builder.WorkspaceToolModelBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * Created by covers1624 on 29/05/19.
 */
@VersionedClass (1)
public class WorkspaceToolGradlePlugin implements Plugin<Gradle> {

    @Override
    public void apply(Gradle gradle) {
        gradle.rootProject(project -> {
            ProjectInternal internal = (ProjectInternal) project;
            ToolingModelBuilderRegistry registry = internal.getServices().get(ToolingModelBuilderRegistry.class);
            registry.register(new WorkspaceToolModelBuilder());
        });
    }
}
