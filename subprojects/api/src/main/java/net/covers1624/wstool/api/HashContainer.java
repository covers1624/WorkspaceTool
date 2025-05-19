package net.covers1624.wstool.api;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.HashUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 1/13/25.
 */
@SuppressWarnings ("UnstableApiUsage")
public class HashContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashContainer.class);

    private static final Gson GSON = new Gson();

    private final Path hashFile;
    private final Container container;

    public HashContainer(Path dir, String name) {
        this.hashFile = dir.resolve(name + ".json");
        Container container = null;
        if (Files.exists(hashFile)) {
            try {
                container = JsonUtils.parse(GSON, hashFile, Container.class);
            } catch (IOException ex) {
                LOGGER.error("Unable to load hashes from {}.", hashFile, ex);
            }
        }
        if (container == null) {
            container = new Container(new LinkedHashMap<>(), new LinkedHashMap<>());
        }
        this.container = container;
    }

    /**
     * Get an entry from this container.
     *
     * @param key The key.
     * @return The entry.
     */
    public Entry getEntry(String key) {
        return new Entry(key, container.hashes.get(key));
    }

    /**
     * Get a saved property.
     * <p>
     * These are separate to hash entries and may contain any user data.
     *
     * @param property The property.
     * @return It's value.
     */
    public @Nullable String getProperty(String property) {
        return getProperty(property, null);
    }

    /**
     * Set a saved property.
     * <p>
     * These are separate to hash entries and may contain any user data.
     *
     * @param property The property.
     * @param value    The property value.
     * @return The previously stored value, if any.
     */
    public @Nullable String setProperty(String property, @Nullable String value) {
        String existing = container.properties.put(property, value);
        save();
        return existing;
    }

    /**
     * Remove a saved property.
     * <p>
     * These are separate to hash entries and may contain any user data.
     *
     * @param property The property.
     * @return The previously stored value, if any.
     */
    public @Nullable String removeProperty(String property) {
        String existing = container.properties.remove(property);
        save();
        return existing;
    }

    /**
     * Get a saved property. These are separate to hash entries and may
     * contain any user data.
     *
     * @param property The property.
     * @return It's value.
     */
    @Contract ("_,!null->!null")
    public @Nullable String getProperty(String property, @Nullable String defaultValue) {
        return container.properties.getOrDefault(property, defaultValue);
    }

    private void storeAndSave(String key, String value) {
        container.hashes.put(key, value);
        save();
    }

    private void save() {
        try {
            JsonUtils.write(GSON, IOUtils.makeParents(hashFile), container);
        } catch (IOException ex) {
            LOGGER.error("Failed to save hashes into {}.", hashFile, ex);
        }
    }

    /**
     * Represents an entry within the container.
     * <p>
     * Add files to hash via the various 'put' methods.
     * <p>
     * Each time {@link #changed()} is called, the hash will be computed and compared against the existing value.
     * <p>
     * Re-run the hash computation and push the new hash back to the container with {@link #pushChanges()}.
     */
    public class Entry {

        public final String key;
        private final @Nullable String value;

        private final List<Consumer<Hasher>> actions = new ArrayList<>();

        public Entry(String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        private String result() {
            Hasher hasher = Hashing.sha256().newHasher();
            actions.forEach(e -> e.accept(hasher));
            return hasher.hash().toString();
        }

        /**
         * Add a string to the hash.
         *
         * @param str The string.
         * @return The same entry.
         */
        public Entry putString(String str) {
            actions.add(h -> h.putString(str, StandardCharsets.UTF_8));
            return this;
        }

        /**
         * Add a file to the in-progress hash.
         *
         * @param file The file.
         * @return The same entry.
         */
        public Entry putFile(Path file) {
            actions.add(h -> {
                try {
                    if (Files.exists(file)) {
                        HashUtils.addToHasher(h, file);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to read file for hashing. " + file, ex);
                }
            });
            return this;
        }

        /**
         * Finalize the in-progress hash, and check if it is different from the stored hash.
         *
         * @return If the hashes were different.
         */
        public boolean changed() {
            return !result().equals(value);
        }

        /**
         * Push the finalized hash into the container and save the result.
         */
        public void pushChanges() {
            storeAndSave(key, result());
        }
    }

    private record Container(
            Map<String, String> hashes,
            Map<String, String> properties
    ) { }
}
