package net.covers1624.wstool.minecraft;

import com.google.gson.Gson;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 6/2/25.
 */
public record AssetIndexManifest(
        @Nullable Map<String, Asset> objects,
        boolean virtual
) {

    private static final Gson GSON = new Gson();

    public static AssetIndexManifest update(HttpEngine http, Path assetsDir, VersionManifest version) throws IOException {
        var index = version.assetIndex();
        Path file = assetsDir.resolve("indexes/" + index.id() + ".json");

        DownloadAction action = new HttpEngineDownloadAction(http)
                .setUrl(index.url())
                .setDest(file)
                .setQuiet(false)
                .setUseETag(true)
                .setOnlyIfModified(true);
        action.execute();
        return JsonUtils.parse(GSON, file, AssetIndexManifest.class);
    }

    @Override
    public Map<String, Asset> objects() {
        return objects != null ? objects : Map.of();
    }

    public record Asset(
            @Nullable String hash,
            @Nullable Long size
    ) {

        @Override
        public String hash() {
            return requireNonNull(hash);
        }

        public String path() {
            return hash().substring(0, 2) + "/" + hash();
        }

        @Override
        public Long size() {
            return requireNonNull(size);
        }
    }
}
