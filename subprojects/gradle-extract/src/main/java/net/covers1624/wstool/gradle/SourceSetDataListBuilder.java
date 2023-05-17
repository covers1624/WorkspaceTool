package net.covers1624.wstool.gradle;

import net.covers1624.quack.collection.FastStream;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtensionsSchema;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 16/5/23.
 */
public class SourceSetDataListBuilder implements DataBuilder {

    @Override
    public void buildProjectData(Project project, ProjectData projectData) {
        SourceSetContainer sourceSets = getSourceSetContainer(project);
        if (sourceSets == null) return;
        for (SourceSet sourceSet : sourceSets) {
            SourceSetData data = new SourceSetData(sourceSet.getName(), sourceSet.getCompileClasspathConfigurationName(), sourceSet.getRuntimeClasspathConfigurationName());
            data.sourceMap.put("resources", getFiles(sourceSet.getResources()));
            data.sourceMap.put("java", getFiles(sourceSet.getJava()));

            addSourcesFromExtension(sourceSet, data);
            addSourcesFromConvention(sourceSet, data);
        }
    }

    private static void addSourcesFromExtension(SourceSet sourceSet, SourceSetData data) {
        // Only extension aware for Gradle 5 >=
        // noinspection ConstantValue
        if (sourceSet instanceof ExtensionAware) {
            @SuppressWarnings ("RedundantCast") // Not redundant.
            ExtensionAware extensionAware = (ExtensionAware) sourceSet;
            // Automatically find Scala/Groovy/Kotlin source sets.
            ExtensionContainer extensions = extensionAware.getExtensions();
            for (ExtensionsSchema.ExtensionSchema schema : extensions.getExtensionsSchema()) {
                String name = schema.getName();
                Object extension = extensions.findByName(name);
                if (!data.sourceMap.containsKey(name) && extension instanceof SourceDirectorySet) {
                    data.sourceMap.put(name, getFiles((SourceDirectorySet) extension));
                }
            }
        }
    }

    private static void addSourcesFromConvention(SourceSet sourceSet, SourceSetData data) {
        // For Gradle < 5 where we can't get this from the SourceSet's extensions.
        // The ScalaSourceSet convention was removed in Gradle 8.
        try {
            Object convention = InvokerHelper.getProperty(sourceSet, "convention");
            if (convention == null) return;

            Map<String, Object> plugins = unsafeCast(InvokerHelper.getProperty(convention, "plugins"));
            Object scalaSS = plugins.get("scala");
            if (scalaSS != null && !data.sourceMap.containsKey("scala")) {
                data.sourceMap.put("scala", getFiles(unsafeCast(InvokerHelper.getProperty(scalaSS, "scala"))));
            }
        } catch (Throwable ignored) {
        }
    }

    private static List<File> getFiles(SourceDirectorySet set) {
        return FastStream.of(set.getSrcDirs()).map(File::getAbsoluteFile).toList();
    }

    @Nullable
    private static SourceSetContainer getSourceSetContainer(Project project) {
        // In Gradle 7.1 all of JavaPluginConvention was mirrored onto JavaPluginExtension, prefer that path.
        if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) >= 0) {
            return getSourceSetContainerExtension(project);
        }
        // Legacy, we need to jump through the Convention.
        return getSourceSetContainerConvention(project);
    }

    @Nullable
    private static SourceSetContainer getSourceSetContainerExtension(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        return java != null ? java.getSourceSets() : null;
    }

    @Nullable
    private static SourceSetContainer getSourceSetContainerConvention(Project project) {
        // JavaPluginConvention was removed in Gradle 8.
        try {
            Object convention = InvokerHelper.getProperty(project, "convention");
            Method findPlugin = convention.getClass().getMethod("findPlugin", Class.class);

            Object javaConvention = findPlugin.invoke(convention, Class.forName("org.gradle.api.plugins.JavaPluginConvention"));
            return (SourceSetContainer) InvokerHelper.getProperty(javaConvention, "sourceSets");
        } catch (Throwable e) {
            return null;
        }
    }
}
