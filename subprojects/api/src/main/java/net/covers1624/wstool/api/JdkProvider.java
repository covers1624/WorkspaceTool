package net.covers1624.wstool.api;

import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.jdkutils.locator.JavaLocator;
import net.covers1624.jdkutils.provisioning.adoptium.AdoptiumProvisioner;
import net.covers1624.quack.net.httpapi.java11.Java11HttpEngine;
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

    public JdkProvider(Environment env) {
        locator = JavaLocator.builder()
                .useJavaw()
                .findGradleJdks()
                .findIntellijJdks()
                .ignoreOpenJ9()
                .build();
        installer = new JdkInstallationManager(env.systemFolder().resolve("jdks/"), new AdoptiumProvisioner(Java11HttpEngine.create()));

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
