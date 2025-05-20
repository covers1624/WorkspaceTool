package net.covers1624.wstool.gradle.api.data;

/**
 * Created by covers1624 on 5/20/25.
 */
public class JavaToolchainData extends Data {

    public final int langVersion;

    public JavaToolchainData(int langVersion) {
        this.langVersion = langVersion;
    }
}
