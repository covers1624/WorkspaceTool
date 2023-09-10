package net.covers1624.wstool.gradle.api.data;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 15/5/23.
 */
public class ProjectData extends Data implements Serializable {

    /**
     * The project name.
     */
    public final String name;
    /**
     * The directory for this project.
     */
    public final File projectDir;

    /**
     * The parent project. If this project is a subproject.
     */
    @Nullable
    public final ProjectData parent;

    /**
     * The project version.
     */
    public final String version;
    /**
     * The project group.
     */
    public final String group;
    /**
     * The project archivesBaseName.
     */
    // TODO is there another property we should use instead? Should we grab the publishing info instead?
    public final String archivesBaseName;

    public ProjectData(String name, File projectDir, @Nullable ProjectData parent, String version, String group, String archivesBaseName) {
        this.name = name;
        this.projectDir = projectDir;
        this.parent = parent;
        this.version = version;
        this.group = group;
        this.archivesBaseName = archivesBaseName;
    }
}
