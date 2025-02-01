package net.covers1624.wstool.api;

import net.covers1624.jdkutils.*;
import net.covers1624.quack.net.java.JavaDownloadAction;
import net.covers1624.quack.util.LazyValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by covers1624 on 2/9/23.
 */
public final class JdkProvider {

    private final JavaLocator locator;
    private final JdkInstallationManager installer;

    private final LazyValue<List<JavaInstall>> installs;

    public JdkProvider(WorkspaceToolEnvironment env) {
        locator = JavaLocator.builder()
                .useJavaw()
                .findGradleJdks()
                .findIntellijJdks()
                .ignoreOpenJ9()
                .build();
        installer = new JdkInstallationManager(env.systemFolder().resolve("jdks/"), new AdoptiumProvisioner(JavaDownloadAction::new));

        installs = new LazyValue<>(() -> {
            try {
                return locator.findJavaVersions();
            } catch (IOException ex) {
                throw new RuntimeException("Unable to find java versions.", ex);
            }
        });
    }

    public Path findOrProvisionJdk(JavaVersion version) {
        for (JavaInstall javaInstall : installs.get()) {
            if (javaInstall.hasCompiler && javaInstall.langVersion == version) {
                return javaInstall.javaHome;
            }
        }
        try {
            return installer.provisionJdk(new JdkInstallationManager.ProvisionRequest.Builder()
                    .forVersion(version)
                    .build()
            );
        } catch (IOException ex) {
            throw new RuntimeException("Failed to provision JDK.", ex);
        }
    }
}
