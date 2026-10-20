package net.covers1624.wstool.gradle;

import groovy.lang.MetaProperty;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by covers1624 on 10/20/26.
 */
public class GradleHelpers {

    public static @Nullable Object findProperty(@Nullable Object obj, String property) {
        if (obj == null) return null;

        MetaProperty mp = InvokerHelper.getMetaClass(obj).hasProperty(obj, property);
        if (mp != null) {
            return unwrapProvider(mp.getProperty(obj));
        }

        if (obj instanceof ExtensionAware) {
            ExtraPropertiesExtension extra = ((ExtensionAware) obj).getExtensions().getExtraProperties();
            if (extra.has(property)) {
                return unwrapProvider(extra.get(property));
            }
        }
        return null;
    }

    public static @Nullable Object unwrapProvider(@Nullable Object value) {
        if (value instanceof Provider) {
            return ((Provider<?>) value).get();
        }
        return value;
    }

    public static List<File> unwrapFileCollection(@Nullable Object obj) {
        if (obj instanceof FileCollection) {
            List<File> files = new ArrayList<>(((FileCollection) obj).getFiles());
            files.sort(Comparator.naturalOrder());
            return files;
        }
        return Collections.emptyList();
    }

    public static List<File> unwrapAsFiles(Project project, @Nullable Object obj) {
        if (obj instanceof Iterable<?>) {
            List<File> files = new ArrayList<>();
            for (Object o : ((Iterable<?>) obj)) {
                files.add(project.file(o));
            }
            return files;
        }
        return Collections.emptyList();
    }
}
