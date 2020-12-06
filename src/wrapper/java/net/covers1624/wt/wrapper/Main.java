package net.covers1624.wt.wrapper;

import net.covers1624.wt.wrapper.json.WrapperProperties;
import net.covers1624.wt.wrapper.maven.MavenNotation;
import net.covers1624.wt.wrapper.maven.MavenResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Created by covers1624 on 20/8/20.
 */
public class Main {

    private static final Path wtFolder = Paths.get(System.getProperty("user.home"), ".workspace_tool")//
            .normalize().toAbsolutePath();

    public static void main(String[] args) throws Throwable {
        Logger logger = LoggerFactory.getLogger("Wrapper");
        logger.info("Preparing WorkspaceTool..");
        Path workspaceProps = Paths.get(".workspace_tool/properties.json");
        WrapperProperties properties = WrapperProperties.compute(workspaceProps);
        URLClassLoader classLoader = computeClasspath(properties);
        Thread.currentThread().setContextClassLoader(classLoader);
        Class<?> clazz = Class.forName(properties.mainClass, true, classLoader);
        logger.info("Launching..");
        System.out.println();
        System.out.println();
        clazz.getMethod("main", String[].class).invoke(null, (Object) args);
    }

    public static URLClassLoader computeClasspath(WrapperProperties properties) throws Exception {
        MavenResolver resolver = new MavenResolver(wtFolder.resolve("local_repo"), properties.repos);
        Set<File> deps = resolver.resolve(MavenNotation.parse(properties.artifact));
        return new URLClassLoader(deps.stream()
                .map(Main::toURL)
                .toArray(URL[]::new)
        );
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
