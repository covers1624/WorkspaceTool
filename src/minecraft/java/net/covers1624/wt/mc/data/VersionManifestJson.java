package net.covers1624.wt.mc.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by covers1624 on 5/02/19.
 */
public class VersionManifestJson {

    private static final Gson gson = new Gson();

    public Latest latest;
    public List<Version> versions = new ArrayList<>();

    public Optional<Version> findVersion(String version) {
        return versions.stream()//
                .filter(v -> v.id.equalsIgnoreCase(version))//
                .findFirst();
    }

    public static class Latest {

        public String release;
        public String snapshot;
    }

    public static class Version {

        public String id;
        public String type;
        public String url;
        public String time;
        public String releaseTime;
    }

}
