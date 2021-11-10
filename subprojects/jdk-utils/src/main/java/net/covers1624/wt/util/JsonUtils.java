/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.covers1624.quack.maven.MavenNotation;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 31/10/21.
 */
// TODO Move to Quack, somehow.
public class JsonUtils {

    public static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MavenNotation.class, new MavenNotationAdapter())
            .setPrettyPrinting()
            .create();

    public static <T> T parse(Path path, Type t) {
        try {
            return parse(Files.newInputStream(path), t);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json file: " + path, e);
        }
    }

    public static <T> T parse(InputStream is, Type t) {
        try (Reader reader = new InputStreamReader(is)) {
            return GSON.fromJson(reader, t);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json.", e);
        }
    }

    public static void write(Path path, Object instance) {
        write(path, instance, instance.getClass());
    }

    public static void write(Path path, Object instance, Type t) {
        try {
            write(Files.newOutputStream(path), instance, t);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write json file: " + path, e);
        }
    }

    public static void write(OutputStream os, Object instance) {
        write(os, instance, instance.getClass());
    }

    public static void write(OutputStream os, Object instance, Type t) {
        try (Writer writer = new OutputStreamWriter(os)) {
            GSON.toJson(instance, t, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write json.", e);
        }
    }

}
