package net.covers1624.wt;

import java.io.IOException;

/**
 * Created by covers1624 on 13/6/19.
 */
public class TestStuff {

    public static void main(String[] args) throws IOException {
        //        try (GradleManagerImpl gradleManager = new GradleManagerImpl()) {
        //            gradleManager.includeClassMarker("net.covers1624.wt.forge.gradle.ForgeGradleDataBuilder");
        //            gradleManager.addDataBuilder("net.covers1624.wt.forge.gradle.ForgeGradleDataBuilder");
        //            gradleManager.executeBefore("genSrgs", "mergeJars");
        //            //            try (GradleExecutor gradleExecutor = new GradleExecutor(gradleManager, Paths.get("CodeChicken/CodeChickenLib"))) {
        //            //                WorkspaceToolModel workspaceToolModel = gradleExecutor.getWorkspaceToolModel();
        //            //                System.out.println();
        //            //            }
        //        }
    }

    public static String commonPath(String... paths) {
        StringBuilder commonPath = new StringBuilder();
        String[][] folders = new String[paths.length][];

        for (int i = 0; i < paths.length; i++) {
            folders[i] = paths[i].split("/");
        }

        for (int j = 0; j < folders[0].length; j++) {
            String s = folders[0][j];
            for (int i = 1; i < paths.length; i++) {
                if (!s.equals(folders[i][j])) {
                    return commonPath.toString();
                }
            }
            commonPath.append(s).append("/");
        }
        return commonPath.toString();
    }
}
