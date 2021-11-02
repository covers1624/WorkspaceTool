package net.covers1624.wt.api.script.runconfig;

import java.util.Map;

/**
 * A simple container that holds RunConfigurations.
 *
 * RunConfigurations are built with GroovyMagic.
 *
 * <pre>
 *     runConfigs {
 *         "Client Run" {
 *             // Configure RunConfiguration.
 *         }
 *     }
 * </pre>
 *
 * Created by covers1624 on 23/7/19.
 */
public interface RunConfigContainer {

    /**
     * @return Gets the RunConfigurations in this Container.
     */
    Map<String, RunConfig> getRunConfigs();
}
