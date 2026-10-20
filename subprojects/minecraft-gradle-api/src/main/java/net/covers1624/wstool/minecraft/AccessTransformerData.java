package net.covers1624.wstool.minecraft;

import net.covers1624.wstool.gradle.api.data.Data;
import net.covers1624.wstool.gradle.api.data.ProjectData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by covers1624 on 10/20/26.
 */
public class AccessTransformerData extends Data {

    public final List<File> files = new ArrayList<>();

    public static void addFiles(ProjectData data, Collection<File> files) {
        if (files.isEmpty()) return;

        AccessTransformerData atData = data.getOrCreateData(AccessTransformerData.class, AccessTransformerData::new);

        atData.files.addAll(files);
    }
}
