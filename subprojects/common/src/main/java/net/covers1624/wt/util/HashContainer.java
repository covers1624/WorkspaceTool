/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import com.google.common.hash.HashCode;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A simple container for Hashes.
 *
 * Created by covers1624 on 5/01/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class HashContainer {

    private static final Logger logger = LogManager.getLogger("HashContainer");
    private static final Type gsonType = new TypeToken<Map<String, HashCode>>() {}.getType();

    private final Path hashFile;
    public Map<String, HashCode> hashes;

    public HashContainer(File hashFile) {
        this(hashFile.toPath());
    }

    public HashContainer(Path hashFile) {
        this.hashFile = hashFile;
        if (Files.exists(hashFile)) {
            hashes = Utils.fromJson(hashFile, gsonType);
        }
        //If the file exists with no content, gson will parse that as a json null.
        //causing hashes to be null.
        if (hashes == null) {
            hashes = new HashMap<>();
        }
    }

    /**
     * Checks if the provided HashCode does not match
     * the stored value for the specified key.
     *
     * @param key   The key.
     * @param other The HashCode to check.
     * @return If the HashCodes do not match.
     */
    public boolean check(String key, HashCode other) {
        return !Objects.equals(hashes.get(key), other);
    }

    /**
     * Get the HashCode at the specified key.
     *
     * @param key The key.
     * @return The HashCode.
     */
    public HashCode get(String key) {
        return hashes.get(key);
    }

    /**
     * Sets the HashCode at a specific key.
     *
     * @param key      The key.
     * @param hashCode The HashCode.
     */
    public void set(String key, HashCode hashCode) {
        hashes.put(key, hashCode);
        save();
    }

    public void remove(String key) {
        hashes.remove(key);
        save();
    }

    /**
     * Saves the container to disk.
     */
    public void save() {
        Utils.toJson(hashes, gsonType, hashFile);
    }
}
