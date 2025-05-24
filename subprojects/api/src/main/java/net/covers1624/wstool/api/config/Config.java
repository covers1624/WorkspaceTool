package net.covers1624.wstool.api.config;

import com.google.gson.annotations.SerializedName;
import net.covers1624.wstool.api.extension.FrameworkType;
import net.covers1624.wstool.api.extension.WorkspaceType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Created by covers1624 on 19/9/24.
 */
public record Config(
        @Nullable List<FrameworkType> frameworks,
        @SerializedName("gradle_hashables")
        @Nullable List<String> gradleHashables,
        @Nullable List<String> modules,
        @Nullable WorkspaceType workspace
) {

    @Override
    public List<FrameworkType> frameworks() {
        return frameworks != null ? frameworks : List.of();
    }

    @Override
    public List<String> gradleHashables() {
        return gradleHashables != null ? gradleHashables : List.of();
    }

    @Override
    public List<String> modules() {
        return modules != null ? modules : List.of();
    }

    @Override
    public WorkspaceType workspace() {
        return Objects.requireNonNull(workspace, "Config requires top-level `workspace` section.");
    }
}
