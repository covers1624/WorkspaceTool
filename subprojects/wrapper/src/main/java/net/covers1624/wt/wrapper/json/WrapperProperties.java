/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.json;

import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 20/8/20.
 */
public class WrapperProperties {

    public MavenNotation artifact;
    public String mirror;

    public static WrapperProperties load(Path workspacePropsPath) {
        WrapperProperties props = JsonUtils.parse(WrapperProperties.class.getResourceAsStream("/properties.json"), WrapperProperties.class);
        if (!Files.exists(workspacePropsPath)) {
            return props;
        }

        // If the user has a '.workspace_tool/properties.json', override artifact and mirror.
        WrapperProperties userProps = JsonUtils.parse(workspacePropsPath, WrapperProperties.class);
        if (userProps.artifact != null) {
            props.artifact = userProps.artifact;
        }
        if (userProps.mirror != null) {
            props.mirror = userProps.mirror;
        }
        return props;
    }
}
