/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.api.impl.script.runconfig;

import net.covers1624.wt.api.script.runconfig.RunConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 23/7/19.
 */
public class DefaultRunConfig implements RunConfig {

    private String mainClass;
    private final List<String> vmArgs = new ArrayList<>();
    private final List<String> progArgs = new ArrayList<>();
    private Path runDir;
    private final Map<String, String> envVars = new HashMap<>();
    private final Map<String, String> sysProps = new HashMap<>();

    //@formatter:off
    @Override public void mainClass(String name) { mainClass = name; }
    @Override public String getMainClass() { return mainClass; }
    @Override public void vmArg(List<Object> args) { args.stream().map(RunConfig::toString).forEach(vmArgs::add); }
    @Override public List<String> getVmArgs() { return vmArgs; }
    @Override public void progArg(List<Object> args) { args.stream().map(RunConfig::toString).forEach(progArgs::add); }
    @Override public List<String> getProgArgs() { return progArgs; }
    @Override public void runDir(Path path) { runDir = path; }
    @Override public Path getRunDir() { return runDir; }
    @Override public void envVar(Map<String, Object> vars) { vars.forEach((key, value) -> envVars.put(key, RunConfig.toString(value))); }
    @Override public Map<String, String> getEnvVars() { return envVars; }
    @Override public void sysProp(Map<String, Object> props) { props.forEach((key, value) -> sysProps.put(key, RunConfig.toString(value))); }
    @Override public Map<String, String> getSysProps() { return sysProps; }
    //@formatter:on
}