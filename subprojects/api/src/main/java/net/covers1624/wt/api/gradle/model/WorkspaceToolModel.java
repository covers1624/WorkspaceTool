/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.gradle.model;

import net.covers1624.wt.api.event.VersionedClass;
import net.covers1624.wt.api.gradle.data.ProjectData;

import java.io.Serializable;

/**
 * Data Model for data extracted from Gradle.
 * <p>
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public interface WorkspaceToolModel extends Serializable {

    /**
     * @return the {@link ProjectData} instance.
     */
    ProjectData getProjectData();
}
