package net.covers1624.wstool.api.workspace.runs;

import net.covers1624.wstool.api.workspace.SourceSet;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by covers1624 on 5/25/25.
 */
public interface RunConfig {

    String name();

    Map<String, String> config();

    @Nullable Path runDir();

    void runDir(Path runDir);

    @Nullable SourceSet classpath();

    void classpath(SourceSet classpath);

    @Nullable String mainClass();

    void mainClass(String mainClass);

    EvalList args();

    EvalList vmArgs();

    EvalMap sysProps();

    EvalMap envVars();
}
