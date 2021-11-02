/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle.builder;

import net.covers1624.wt.api.gradle.data.BuildProperties;
import net.covers1624.wt.api.event.VersionedClass;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

/**
 * Simple wrapper for ToolingModelBuilder.
 * Created by covers1624 on 15/6/19.
 */
@VersionedClass (1)
public abstract class AbstractModelBuilder<T> implements ParameterizedToolingModelBuilder<BuildProperties> {

    private final Class<T> clazz;

    protected AbstractModelBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    public abstract T build(Project project, BuildProperties properties) throws Exception;

    @Override
    public boolean canBuild(String modelName) {
        return clazz.getName().equals(modelName);
    }

    @Override
    public Class<BuildProperties> getParameterType() {
        return BuildProperties.class;
    }

    @Override
    public Object buildAll(String modelName, BuildProperties properties, Project project) {
        try {
            return build(project, properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        return buildAll(modelName, null, project);
    }
}
