package net.covers1624.wstool.neoforge.gradle;

import net.covers1624.wstool.gradle.ProjectTransformer;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Attribute;

/**
 * Created by covers1624 on 10/18/25.
 */
public class NeoDevProjectTransformer implements ProjectTransformer {

    @Override
    public void transformProject(Project project, ProjectData projectData) {
        // TODO, NeoForge 1.21.4 requires this hack otherwise we can't extract dependencies from the coremods sub-project
        //       due to a variant resolution conflict. I have no idea why this is not an issue for Intellij. I don't know why
        //       a regular build doesn't break things. ./gradlew :neoforge-coremods:compileJava is able to reproduce the issue.
        if (project.getName().equals("neoforge-coremods") && project.getParent() != null) {
            ConfigurationContainer configurations = project.getConfigurations();
            configurations.getByName("compileClasspath").attributes(e -> {
                // Just select the server variant, I don't think it really matters much.
                e.attribute(Attribute.of("net.neoforged.distribution", String.class), "server");
            });
        }
    }
}
