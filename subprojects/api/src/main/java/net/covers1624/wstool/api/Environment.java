package net.covers1624.wstool.api;

import net.covers1624.quack.util.SneakyUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 17/5/23.
 */
public interface Environment {

    /**
     * @return The global system directory for Workspace Tool.
     */
    Path systemFolder();

    /**
     * @return The root directory of the project.
     */
    Path projectRoot();

    /**
     * @return The cache directory inside the root project.
     */
    Path projectCache();

    /**
     * Provide some typed service to the environment.
     *
     * @param clazz The type of the service.
     * @param thing The service.
     */
    <T> void putService(Class<? extends T> clazz, T thing);

    /**
     * Get some typed service from the environment.
     *
     * @param clazz The service type.
     * @return The service.
     */
    <T> T getService(Class<? extends T> clazz);

    static Environment of() {
        return of(
                Path.of(System.getProperty("user.home"), ".workspace_tool"),
                Path.of(".").toAbsolutePath().normalize()
        );
    }

    static Environment of(Path sysFolder, Path projectRoot) {
        return of(sysFolder, projectRoot, projectRoot.resolve(".wstool/"));
    }

    static Environment of(Path sysFolder, Path projectRoot, Path projectCache) {
        Map<Class<?>, Object> serviceMap = new HashMap<>();
        return new Environment() {
            // @formatter:off
            @Override public Path systemFolder() { return sysFolder; }
            @Override public Path projectRoot() { return projectRoot; }
            @Override public Path projectCache() { return projectCache; }
            // @formatter:on

            @Override
            public <T> void putService(Class<? extends T> clazz, T thing) {
                if (serviceMap.containsKey(clazz)) throw new IllegalArgumentException("Unable to replace a service.");
                serviceMap.put(clazz, thing);
            }

            @Override
            public <T> T getService(Class<? extends T> clazz) {
                Object service = serviceMap.get(clazz);
                if (service == null) throw new IllegalStateException("Service " + clazz.getName() + " is not available.");

                return SneakyUtils.unsafeCast(service);
            }
        };
    }
}
