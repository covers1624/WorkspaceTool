/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
