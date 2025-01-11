package net.covers1624.wstool.gradle.api;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;

import java.io.File;

/**
 * This is serialized by Gradle and sent to the daemon.
 * <p>
 * This class must be compiled with Java 8, otherwise old gradle will crash.
 * <p>
 * Created by covers1624 on 1/9/23.
 */
public class WorkspaceToolModelAction implements BuildAction<WorkspaceToolModel> {

    private final File cacheFile;

    public WorkspaceToolModelAction(File cacheFile) {
        this.cacheFile = cacheFile;
    }

    @Override
    public WorkspaceToolModel execute(BuildController controller) {
        return controller.getModel(WorkspaceToolModel.class, ModelProperties.class, p -> {
            p.setOutputFile(cacheFile);
        });
    }
}
