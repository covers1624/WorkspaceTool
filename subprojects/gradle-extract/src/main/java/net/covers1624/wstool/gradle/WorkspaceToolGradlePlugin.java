package net.covers1624.wstool.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * Created by covers1624 on 14/5/23.
 */
public class WorkspaceToolGradlePlugin implements Plugin<Gradle> {

    @Override
    public void apply(Gradle gradle) {
        gradle.rootProject(project -> {
            ProjectInternal internal = (ProjectInternal) project;
            internal.getServices().get(ToolingModelBuilderRegistry.class)
                    .register(new WorkspaceToolModelBuilder());
        });
    }
}
