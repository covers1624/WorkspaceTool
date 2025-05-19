package net.covers1624.wstool.api.config;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created by covers1624 on 21/10/24.
 */
public record RunConfigTemplate(
        @Nullable String name,
        @SerializedName ("template")
        @Nullable String templateName,
        @Nullable Map<String, String> config,
        @Nullable List<String> from,
        @SerializedName ("run_dir")
        @Nullable String runDir,
        @SerializedName ("main_class")
        @Nullable String mainClass,
        @Nullable List<String> args,
        @SerializedName ("sys_props")
        @Nullable Map<String, String> sysProps,
        @Nullable Map<String, String> env
) {

    @Override
    public Map<String, String> config() {
        return config != null ? config : Map.of();
    }

    @Override
    public List<String> from() {
        return from != null ? from : List.of();
    }

    @Override
    public List<String> args() {
        return args != null ? args : List.of();
    }

    @Override
    public Map<String, String> sysProps() {
        return sysProps != null ? sysProps : Map.of();
    }

    @Override
    public Map<String, String> env() {
        return env != null ? env : Map.of();
    }
}
