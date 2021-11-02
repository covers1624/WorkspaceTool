package net.covers1624.wt.wrapper.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.gson.PathTypeAdapter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 31/10/21.
 */
public class JsonUtils {

    public static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new PathTypeAdapter())
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
