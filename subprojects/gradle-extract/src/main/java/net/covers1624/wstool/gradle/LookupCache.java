package net.covers1624.wstool.gradle;

import net.covers1624.wstool.gradle.api.data.ProjectData;
import net.covers1624.wstool.gradle.api.data.SourceSetData;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 9/9/23.
 */
public class LookupCache {

    public final Map<Project, ProjectData> projects = new HashMap<>();
    public final Map<SourceSetOutput, SourceSetData> sourceSets = new HashMap<>();
}
