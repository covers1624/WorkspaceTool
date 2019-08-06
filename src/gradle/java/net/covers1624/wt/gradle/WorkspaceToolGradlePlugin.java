package net.covers1624.wt.gradle;

import com.google.common.collect.ImmutableMap;
import net.covers1624.gradlestuff.sourceset.SourceSetDependencyPlugin;
import net.covers1624.wt.gradle.builder.WorkspaceToolModelBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * Created by covers1624 on 29/05/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class WorkspaceToolGradlePlugin implements Plugin<Gradle> {

    @Override
    public void apply(Gradle gradle) {
        gradle.rootProject(project -> {
            project.apply(ImmutableMap.of("plugin", "java"));
            project.apply(ImmutableMap.of("plugin", SourceSetDependencyPlugin.class));
            ProjectInternal internal = (ProjectInternal) project;
            ToolingModelBuilderRegistry registry = internal.getServices().get(ToolingModelBuilderRegistry.class);
            registry.register(new WorkspaceToolModelBuilder());
        });
    }
}
