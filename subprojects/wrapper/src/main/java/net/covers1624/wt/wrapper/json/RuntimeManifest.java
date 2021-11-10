/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.json;

import com.google.gson.annotations.SerializedName;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.wt.java.JavaVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 9/11/21.
 */
public class RuntimeManifest {

    @SerializedName ("main_class")
    public String mainClass;

    @SerializedName ("java_version")
    public JavaVersion javaVersion;

    public Map<MavenNotation, Dependency> dependencies = new HashMap<>();

    public static class Dependency {

        public String sha256;
        public int size;
    }
}
