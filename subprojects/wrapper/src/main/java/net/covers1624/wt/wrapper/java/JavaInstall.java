package net.covers1624.wt.wrapper.java;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 30/10/21.
 */
public class JavaInstall {

    public final JavaVersion langVersion;
    public final Path javaHome;
    public final String vendor;
    public final String implName;
    public final String implVersion;
    public final String runtimeName;
    public final String runtimeVersion;
    public final boolean x64;
    public final boolean isOpenJ9;

    public JavaInstall(Path javaHome, String vendor, String implName, String implVersion, String runtimeName, String runtimeVersion, boolean x64) {
        this.langVersion = requireNonNull(JavaVersion.parse(implVersion), "Unable to parse java version: " + implVersion);
        this.javaHome = javaHome;
        this.vendor = vendor;
        this.implName = implName;
        this.implVersion = implVersion;
        this.runtimeName = runtimeName;
        this.runtimeVersion = runtimeVersion;
        this.x64 = x64;
        isOpenJ9 = implName.contains("J9");
    }

    @Override
    public String toString() {
        return "JavaInstall{" +
                "langVersion=" + langVersion +
                ", javaHome=" + javaHome +
                ", vendor='" + vendor + '\'' +
                ", implName='" + implName + '\'' +
                ", implVersion='" + implVersion + '\'' +
                ", runtimeName='" + runtimeName + '\'' +
                ", runtimeVersion='" + runtimeVersion + '\'' +
                ", x64=" + x64 +
                '}';
    }
}
