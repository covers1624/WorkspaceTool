/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.script.runconfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A RunConfiguration, Called by scripts.
 * <p>
 * TODO, Support alternate JREs
 * Created by covers1624 on 23/7/19.
 */
public interface RunConfig {

    /**
     * Sets the MainClass to invoke.
     *
     * @param name The Class name.
     */
    void mainClass(String name);

    /**
     * @return Gets the MainClass to invoke.
     */
    String getMainClass();

    /**
     * Adds Vm Arguments to this RunConfiguration.
     *
     * @param args The Arguments.
     */
    default void vmArg(Object... args) {
        vmArg(Arrays.asList(args));
    }

    /**
     * Adds Vm Arguments to this RunConfiguration.
     *
     * @param args The Arguments.
     */
    void vmArg(List<?> args);

    /**
     * @return Gets the Vm Arguments.
     */
    List<String> getVmArgs();

    /**
     * Adds Program Argument to this RunConfiguration.
     *
     * @param args The Arguments.
     */
    default void progArg(Object... args) {
        progArg(Arrays.asList(args));
    }

    /**
     * Adds Program Argument to this RunConfiguration.
     *
     * @param args The Arguments.
     */
    void progArg(List<?> args);

    /**
     * @return Gets the VmArguments.
     */
    List<String> getProgArgs();

    /**
     * Sets the Run Direcrory for this RunConfiguration.
     *
     * @param path The run directory.
     */
    void runDir(Path path);

    /**
     * @return Gets the Run directory.
     */
    Path getRunDir();

    /**
     * Adds Environment Variables to this Run Configuration.
     *
     * @param vars The variables.
     */
    void envVar(Map<String, ?> vars);

    /**
     * @return Gets the Environment Variables for this RunConfiguration.
     */
    Map<String, String> getEnvVars();

    /**
     * Adds System Properties to this Run Configuration.
     *
     * @param props The properties.
     */
    void sysProp(Map<String, ?> props);

    /**
     * @return Gets the System Properties for this RunConfiguration.
     */
    Map<String, String> getSysProps();
}
