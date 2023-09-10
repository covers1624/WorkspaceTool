package net.covers1624.wstool.gradle.api;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;

import java.io.File;
import java.util.Set;

/**
 * This is serialized by Gradle and sent to the daemon.
 * <p>
 * This class must be compiled with Java 8, otherwise old gradle will crash.
 * <p>
 * Created by covers1624 on 1/9/23.
 */
public class WorkspaceToolModelAction implements BuildAction<WorkspaceToolModel> {

    private final File cacheFile;
    private final Set<String> pluginBuilders;
    private final Set<String> projectBuilders;

    public WorkspaceToolModelAction(File cacheFile, Set<String> pluginBuilders, Set<String> projectBuilders) {
        this.cacheFile = cacheFile;
        this.pluginBuilders = pluginBuilders;
        this.projectBuilders = projectBuilders;
    }

    @Override
    public WorkspaceToolModel execute(BuildController controller) {
        return controller.getModel(WorkspaceToolModel.class, ModelProperties.class, p -> {
            p.setOutputFile(cacheFile);
            p.setPluginBuilders(pluginBuilders);
            p.setProjectBuilders(projectBuilders);
        });
    }
}
