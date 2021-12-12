/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper.json;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by covers1624 on 9/11/21.
 */
public class RuntimeManifest {

    @SerializedName ("main_class")
    public String mainClass;

    @SerializedName ("java_version")
    public JavaVersion javaVersion;

    public List<Dependency> dependencies = new LinkedList<>();

    public static class Dependency {

        @JsonAdapter (MavenNotationAdapter.class)
        public MavenNotation artifact;
        public String sha256;
        public int size;
    }
}
