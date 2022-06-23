/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script.runconfig;

import java.util.Map;

/**
 * A simple container that holds RunConfigurations.
 * <p>
 * RunConfigurations are built with GroovyMagic.
 *
 * <pre>
 *     runConfigs {
 *         "Client Run" {
 *             // Configure RunConfiguration.
 *         }
 *     }
 * </pre>
 * <p>
 * Created by covers1624 on 23/7/19.
 */
public interface RunConfigContainer {

    /**
     * @return Gets the RunConfigurations in this Container.
     */
    Map<String, RunConfig> getRunConfigs();
}
