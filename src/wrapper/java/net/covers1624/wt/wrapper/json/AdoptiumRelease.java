package net.covers1624.wt.wrapper.json;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 30/10/21.
 */
public class AdoptiumRelease {

    private static final Type LIST_TYPE = new TypeToken<List<AdoptiumRelease>>() { }.getType();

    public static List<AdoptiumRelease> parseReleases(Path path) {
        return JsonUtils.parse(path, LIST_TYPE);
    }

    public List<Binary> binaries = new ArrayList<>();
    public String release_name;
    public VersionData version_data;

    public static class Binary {

        @SerializedName ("package")
        public Package _package;
    }

    public static class Package {

        public String checksum;
        public String link;
        public String name;
        public int size;
    }

    public static class VersionData {
        public String semver;
    }
}
