/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.gradle.builder;

import net.covers1624.wt.api.gradle.data.BuildProperties;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by covers1624 on 14/6/19.
 */
public class SimpleBuildAction<T> implements BuildAction<T>, Serializable {

    private final Class<T> modelClazz;
    private final Set<String> dataBuilders;

    public SimpleBuildAction(Class<T> modelClazz, Set<String> dataBuilders) {
        this.modelClazz = modelClazz;
        this.dataBuilders = dataBuilders;
    }

    @Override
    public T execute(BuildController controller) {
        return controller.getModel(modelClazz, BuildProperties.class, e -> e.setDataBuilders(dataBuilders));
    }
}
