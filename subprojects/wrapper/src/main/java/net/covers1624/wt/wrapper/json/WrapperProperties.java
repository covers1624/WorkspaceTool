/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.json;

import com.google.gson.annotations.SerializedName;
import net.covers1624.wt.java.JavaVersion;
import net.covers1624.wt.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/**
 * Created by covers1624 on 20/8/20.
 */
public class WrapperProperties {

    public String artifact;
    @SerializedName ("main_class")
    public String mainClass;
    @SerializedName ("required_java")
    public JavaVersion requiredJava;
    public LinkedHashMap<String, String> repos = new LinkedHashMap<>();

    public WrapperProperties() {
    }

    private WrapperProperties(WrapperProperties other) {
        artifact = other.artifact;
        mainClass = other.mainClass;
        requiredJava = other.requiredJava;
        repos.putAll(other.repos);
    }

    // TODO, is it worth keeping local overrides?
    public static WrapperProperties compute(Path workspacePropsPath) {
        WrapperProperties defaultProps = JsonUtils.parse(WrapperProperties.class.getResourceAsStream("/properties.json"), WrapperProperties.class);
        if (!Files.exists(workspacePropsPath)) {
            return defaultProps;
        }

        // If the user has a '.workspace_tool/properties.json', load additional repo urls.
        WrapperProperties workspaceProps = JsonUtils.parse(workspacePropsPath, WrapperProperties.class);
        WrapperProperties ret = new WrapperProperties(defaultProps);
        ret.repos = new LinkedHashMap<>();
        ret.repos.putAll(workspaceProps.repos);
        ret.repos.putAll(defaultProps.repos);
        return ret;
    }
}
