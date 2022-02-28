/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.json;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 20/8/20.
 */
public class WrapperProperties {

    private static final Gson GSON = new Gson();

    @Nullable
    @JsonAdapter (MavenNotationAdapter.class)
    public MavenNotation artifact;
    @Nullable
    public String mirror;

    public static WrapperProperties load(Path workspacePropsPath) throws IOException {
        WrapperProperties props = JsonUtils.parse(GSON, requireNonNull(WrapperProperties.class.getResourceAsStream("/properties.json")), WrapperProperties.class);
        if (!Files.exists(workspacePropsPath)) {
            return props;
        }

        // If the user has a '.workspace_tool/properties.json', override artifact and mirror.
        WrapperProperties userProps = JsonUtils.parse(GSON, workspacePropsPath, WrapperProperties.class);
        if (userProps.artifact != null) {
            props.artifact = userProps.artifact;
        }
        if (userProps.mirror != null) {
            props.mirror = userProps.mirror;
        }
        return props;
    }
}
