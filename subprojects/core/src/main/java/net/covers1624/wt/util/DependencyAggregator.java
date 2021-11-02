/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.api.WorkspaceToolContext;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.dependency.ScalaSdkDependency;
import net.covers1624.wt.api.module.Configuration;
import net.covers1624.wt.api.module.Module;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by covers1624 on 10/7/19.
 */
public class DependencyAggregator {

    private final WorkspaceToolContext context;
    private final Map<String, String> overrides;

    private final Map<ScalaVersion, NavigableMap<ArtifactVersion, ScalaSdkDependency>> scalaVersionTable = new HashMap<>();
    private final Map<String, NavigableMap<ArtifactVersion, MavenDependency>> versionTable = new HashMap<>();

    public DependencyAggregator(WorkspaceToolContext context) {
        this.context = context;
        overrides = context.workspaceScript.getDepOverrides();
    }

    public void consume(Module module) {
        for (Configuration configuration : module.getConfigurations().values()) {
            configuration.getDependencies().stream()
                    .filter(e -> e instanceof MavenDependency)
                    .map(e -> (MavenDependency) e)
                    .forEach(dep -> {
                        MavenNotation notation = dep.getNotation();
                        Map<ArtifactVersion, MavenDependency> versions = versionTable.computeIfAbsent(getKey(notation), e -> new TreeMap<>());
                        versions.put(new DefaultArtifactVersion(notation.version), dep);
                    });
            configuration.getDependencies().stream()
                    .filter(e -> e instanceof ScalaSdkDependency)
                    .map(e -> (ScalaSdkDependency) e)
                    .forEach(dep -> {
                        Map<ArtifactVersion, ScalaSdkDependency> versions = scalaVersionTable.computeIfAbsent(dep.getScalaVersion(), e -> new TreeMap<>());
                        versions.put(new DefaultArtifactVersion(dep.getVersion()), dep);
                    });
        }
    }

    public MavenDependency resolve(MavenNotation notation) {
        notation = MavenNotation.parse(transformDep(notation.toString()));
        return versionTable.get(getKey(notation)).lastEntry().getValue();
    }

    public ScalaSdkDependency resolveScala(ScalaVersion version) {
        return scalaVersionTable.get(version).lastEntry().getValue();
    }

    private String transformDep(String mavenDep) {
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            mavenDep = mavenDep.replace(entry.getKey(), entry.getValue());
        }
        return mavenDep;
    }

    private String getKey(MavenNotation notation) {
        StringBuilder builder = new StringBuilder(notation.group);
        builder.append("_").append(notation.module);
        if (StringUtils.isNotEmpty(notation.classifier)) {
            builder.append("_").append(notation.classifier);
        }
        return builder.toString();
    }
}
