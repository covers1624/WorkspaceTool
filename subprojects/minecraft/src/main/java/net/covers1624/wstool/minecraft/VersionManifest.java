package net.covers1624.wstool.minecraft;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.LowerCaseEnumAdapterFactory;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 25/10/24.
 */
public record VersionManifest(
        @Nullable Arguments arguments,
        @Nullable AssetIndex assetIndex,
        @Nullable String assets,
        @Nullable Map<String, Download> downloads,
        @Nullable String id,
        @Nullable JavaVersion javaVersion,
        @Nullable List<Library> libraries,
        @Nullable Logging logging,
        @Nullable String mainClass,
        @Nullable String minecraftArguments,
        int minimumLauncherVersion,
        @Nullable Date time,
        @Nullable Date releaseTime,
        @Nullable String type,
        @Nullable String inheritsFrom
) {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OS.class, new OsDeserializer())
            .registerTypeAdapter(MavenNotation.class, new MavenNotationAdapter())
            .create();

    public static CompletableFuture<VersionManifest> updateFuture(HttpEngine http, Path versionsDir, VersionListManifest.Version version) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return update(http, versionsDir, version);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to update version.", ex);
            }
        });
    }

    public static VersionManifest update(HttpEngine http, Path versionsDir, VersionListManifest.Version version) throws IOException {
        Path file = versionsDir.resolve(version.id() + "/" + version.id() + ".json");

        DownloadAction action = new HttpEngineDownloadAction(http)
                .setUrl(version.url())
                .setDest(file)
                .setQuiet(false)
                .setUseETag(true)
                .setOnlyIfModified(true);
        action.execute();
        return JsonUtils.parse(GSON, file, VersionManifest.class);
    }

    public Path requireDownload(HttpEngine http, Path versionsDir, String downloadName, String fileExtension) throws IOException {
        Download download = downloads().get(downloadName);
        if (download == null) throw new RuntimeException("Missing download " + downloadName);

        Path file = versionsDir.resolve(id() + "/" + id() + "-" + downloadName + "." + fileExtension);
        DownloadAction action = new HttpEngineDownloadAction(http)
                .setUrl(download.url())
                .setDest(file)
                .setQuiet(false)
                .setUseETag(true)
                .setOnlyIfModified(true);
        action.execute();
        return file;
    }

    public CompletableFuture<Path> requireDownloadAsync(HttpEngine http, Path versionsDir, String downloadName, String fileExtension) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requireDownload(http, versionsDir, downloadName, fileExtension);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to download file.", ex);
            }
        });
    }

    public boolean hasDownload(String name) {
        return downloads().containsKey(name);
    }

    @Override
    public AssetIndex assetIndex() {
        return requireNonNull(assetIndex);
    }

    @Override
    public String id() {
        return requireNonNull(id);
    }

    @Override
    public String type() {
        return requireNonNull(type);
    }

    @Override
    public Map<String, Download> downloads() {
        return downloads != null ? downloads : Map.of();
    }

    @Override
    public List<Library> libraries() {
        return libraries != null ? libraries : List.of();
    }

    public record Arguments(
            @JsonAdapter (ConditionalEntryDeserializer.class)
            @Nullable List<ConditionalEntry> game,
            @JsonAdapter (ConditionalEntryDeserializer.class)
            @Nullable List<ConditionalEntry> jvm
    ) {

        @Override
        public List<ConditionalEntry> game() {
            return game != null ? game : List.of();
        }

        @Override
        public List<ConditionalEntry> jvm() {
            return jvm != null ? jvm : List.of();
        }
    }

    public record AssetIndex(
            @Nullable String id,
            @Nullable String sha1,
            long size,
            long totalSize,
            @Nullable String url
    ) {

        @Override
        public String id() {
            return requireNonNull(id);
        }

        @Override
        public String url() {
            return requireNonNull(url);
        }
    }

    public record Download(
            @Nullable String sha1,
            long size,
            @Nullable String url
    ) {

        @Override
        public String sha1() {
            return requireNonNull(sha1);
        }

        @Override
        public String url() {
            return requireNonNull(url);
        }
    }

    public record JavaVersion(
            @Nullable String component,
            int majorVersion
    ) { }

    public record Library(
            @Nullable MavenNotation name,
            @Nullable LibraryExtract extract,
            @Nullable LibraryDownloads downloads,
            @Nullable List<Rule> rules,
            @Nullable Map<OS, String> natives,
            @Nullable String url
    ) {

        @Override
        public MavenNotation name() {
            return requireNonNull(name);
        }

        @Override
        public Map<OS, String> natives() {
            return natives != null ? natives : Map.of();
        }
    }

    public record LibraryDownloads(
            @Nullable LibraryArtifact artifact,
            @Nullable Map<String, LibraryArtifact> classifiers
    ) {

        @Override
        public Map<String, LibraryArtifact> classifiers() {
            return classifiers != null ? classifiers : Map.of();
        }
    }

    public record LibraryArtifact(
            @Nullable String path,
            @Nullable String sha1,
            long size,
            @Nullable String url
    ) { }

    public record LibraryExtract(
            @Nullable List<String> exclude
    ) {

        @Override
        public List<String> exclude() {
            return exclude != null ? exclude : List.of();
        }
    }

    public record Rule(
            @JsonAdapter (LowerCaseEnumAdapterFactory.class)
            @Nullable Action action,
            @Nullable OSRule os,
            @Nullable Map<String, Boolean> features
    ) {

        @Override
        public Map<String, Boolean> features() {
            return features != null ? features : Map.of();
        }

        /// Null result indicates this rule does not have any preference.
        public @Nullable Action apply(Set<String> features) {
            // We don't have an OS restriction or the OS does not match.
            if (os != null && !os.applies()) return null;

            if (this.features != null) {
                if (ColUtils.anyMatch(this.features.entrySet(), e -> features.contains(e.getKey()) != e.getValue())) {
                    return null;
                }
            }
            return requireNonNull(action, "Default action can't be null.");
        }

        public static boolean apply(@Nullable List<Rule> rules, Set<String> features) {
            if (rules == null) return true;

            return FastStream.of(rules)
                           .map(e -> e.apply(features))
                           // This mirrors Mojang's logic, Last to return no-preference, takes precedence.
                           .fold(Action.DISALLOW, (a, b) -> b != null ? b : a) == Action.ALLOW;
        }
    }

    public record OSRule(
            @Nullable OS name,
            @Nullable String version,
            @Nullable String arch
    ) {

        public boolean applies() {
            if (name != null && name != OS.current()) return false;
            if (version != null) {
                try {
                    Pattern pattern = Pattern.compile(version);
                    if (!pattern.matcher(System.getProperty("os.version")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (arch != null) {
                try {
                    Pattern pattern = Pattern.compile(arch);
                    if (!pattern.matcher(System.getProperty("os.arch")).matches()) {
                        return false;
                    }
                } catch (Throwable ignored) {
                }
            }
            return true;
        }
    }

    public record Logging(
            @Nullable LoggingEntry client
    ) { }

    public record LoggingEntry(
            @Nullable String argument,
            @Nullable String type,
            @Nullable LoggingDownload file
    ) { }

    public record LoggingDownload(
            @Nullable String id,
            @Nullable String sha1,
            long size,
            @Nullable String url
    ) { }

    public interface ConditionalEntry {

        List<String> get(Set<String> features);
    }

    public record LiteralEntry(List<String> values) implements ConditionalEntry {

        @Override
        public List<String> get(Set<String> features) {
            return values;
        }
    }

    public record RuledEntry(List<String> values, List<Rule> rules) implements ConditionalEntry {

        @Override
        public List<String> get(Set<String> features) {
            if (!Rule.apply(rules, features)) return List.of();

            return values;
        }
    }

    public enum OS {
        WINDOWS,
        LINUX,
        OSX,
        UNKNOWN;

        @Nullable
        private static OS current;

        public static OS current() {
            if (current == null) {
                current = parse(System.getProperty("os.name"));
            }
            return current;
        }

        static OS parse(String name) {
            name = name.toLowerCase(Locale.ROOT);
            if (name.contains("win")) return WINDOWS;
            if (name.contains("mac") || name.contains("osx")) return OSX;
            if (name.contains("linux")) return LINUX;
            return UNKNOWN;
        }
    }

    public enum Action {
        ALLOW,
        DISALLOW,
    }

    public static class ConditionalEntryDeserializer implements JsonDeserializer<List<ConditionalEntry>> {

        private static final Type RULES_LIST = new TypeToken<List<Rule>>() { }.getType();

        @Override
        public List<ConditionalEntry> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<ConditionalEntry> entries = new ArrayList<>();
            if (!json.isJsonArray()) throw new JsonParseException("Expected JsonArray. '" + json + "'");

            for (JsonElement elm : json.getAsJsonArray()) {
                if (elm.isJsonPrimitive()) {
                    entries.add(new LiteralEntry(List.of(elm.getAsString())));
                } else if (elm instanceof JsonObject obj) {
                    JsonElement value = obj.get("value");
                    if (value == null) throw new JsonParseException("Missing 'value' element. '" + obj + "'");
                    if (!value.isJsonPrimitive() && !value.isJsonArray()) throw new JsonParseException("Expected JsonPrimitive or JsonArray. '" + value + "'");

                    List<String> values = value.isJsonPrimitive() ? List.of(value.getAsString()) : getStringList(value.getAsJsonArray());
                    JsonElement rules = obj.get("rules");
                    if (rules == null) throw new JsonParseException("Missing 'rules' element. '" + obj + "'");

                    entries.add(new RuledEntry(values, context.deserialize(rules, RULES_LIST)));
                }
            }
            return entries;
        }

        private static List<String> getStringList(JsonArray array) {
            List<String> values = new ArrayList<>();
            for (JsonElement elm : array) {
                if (!elm.isJsonPrimitive()) throw new JsonParseException("Expected JsonPrimitive '" + array + "'");
                values.add(elm.getAsString());
            }
            return List.copyOf(values);
        }
    }

    public static class OsDeserializer implements JsonDeserializer<OS> {

        @Override
        public OS deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonPrimitive()) throw new JsonParseException("OS requires a JsonPrimitive. Got: " + json);
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (!primitive.isString()) throw new JsonParseException("OS requires a String primitive. Got:" + primitive);
            OS os = OS.parse(primitive.getAsString());
            if (os == OS.UNKNOWN) throw new JsonParseException("Invalid OS. Got: " + primitive.getAsString());
            return os;
        }
    }
}
