package net.covers1624.wstool.minecraft;

import com.google.gson.Gson;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 24/10/24.
 */
public record VersionListManifest(
        @Nullable Latest latest,
        @Nullable List<Version> versions
) {

    private static final Gson GSON = new Gson();
    private static final String URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public static VersionListManifest update(HttpEngine http, Path versionsDir) throws IOException {
        Path file = versionsDir.resolve("version_manifest_v2.json");

        DownloadAction action = new HttpEngineDownloadAction(http)
                .setUrl(URL)
                .setDest(file)
                .setQuiet(false)
                .setUseETag(true)
                .setOnlyIfModified(true);
        action.execute();
        return JsonUtils.parse(GSON, file, VersionListManifest.class);
    }

    @Override
    public Latest latest() {
        return requireNonNull(latest);
    }

    @Override
    public List<Version> versions() {
        return versions != null ? versions : List.of();
    }

    public Map<String, Version> versionsMap() {
        return FastStream.of(versions())
                .toMap(Version::id, Function.identity());
    }

    public record Latest(
            @Nullable String release,
            @Nullable String snapshot
    ) {

        // @formatter:off
        @Override public String release() { return requireNonNull(release); }
        @Override public String snapshot() { return requireNonNull(snapshot); }
        // @formatter:on
    }

    public record Version(
            @Nullable String id,
            @Nullable String type,
            @Nullable String url,
            @Nullable Date time,
            @Nullable Date releaseTime,
            @Nullable String sha1,
            int complianceLevel
    ) {

        // @formatter:off
        @Override public String id() { return requireNonNull(id); }
        @Override public String type() { return requireNonNull(type); }
        @Override public String url() { return requireNonNull(url); }
        @Override public Date time() { return requireNonNull(time); }
        @Override public Date releaseTime() { return requireNonNull(releaseTime); }
        @Override public String sha1() { return requireNonNull(sha1); }
        // @formatter:on
    }
}
