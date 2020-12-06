package net.covers1624.wt.wrapper.json;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 20/8/20.
 */
public class WrapperProperties {

    private static final Gson gson = new Gson();

    public String artifact;
    @SerializedName ("main_class")
    public String mainClass;
    public Map<String, String> repos = new HashMap<>();

    public WrapperProperties() {
    }

    private WrapperProperties(WrapperProperties other) {
        this.artifact = other.artifact;
        this.mainClass = other.mainClass;
        this.repos.putAll(other.repos);
    }

    public static WrapperProperties compute(Path workspacePropsPath) {
        WrapperProperties defaultProps = parse(WrapperProperties.class.getResourceAsStream("/properties.json"), "/properties.json");
        if (!Files.exists(workspacePropsPath)) {
            return defaultProps;
        }
        WrapperProperties workspaceProps = parse(workspacePropsPath);
        WrapperProperties ret = new WrapperProperties(defaultProps);
        if (workspaceProps.artifact != null) {
            ret.artifact = workspaceProps.artifact;
        }
        if (workspaceProps.mainClass != null) {
            ret.mainClass = workspaceProps.mainClass;
        }

        if (!workspaceProps.repos.isEmpty()) {
            ret.repos.clear();
            ret.repos.putAll(workspaceProps.repos);
        }
        return ret;
    }

    public static WrapperProperties parse(Path path) {
        try {
            return parse(Files.newInputStream(path), path.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path.toAbsolutePath(), e);
        }
    }

    private static WrapperProperties parse(InputStream is, String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return gson.fromJson(reader, WrapperProperties.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read WorkspaceTool properties from: " + path, e);
        }
    }

}
