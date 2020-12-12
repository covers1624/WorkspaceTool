package net.covers1624.wt.gradle;

import net.covers1624.wt.api.gradle.GradleManager;
import net.covers1624.wt.util.CopyingFileVisitor;
import net.covers1624.wt.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.covers1624.wt.util.Utils.*;

/**
 * Created by covers1624 on 13/6/19.
 */
public class GradleManagerImpl implements Closeable, GradleManager {

    private static final Logger logger = LogManager.getLogger("GradleManagerImpl");

    private final Set<Object> scriptClasspathMarkerClasses = new HashSet<>();
    private final Set<String> scriptClasspathMarkerResources = new HashSet<>();

    private final Set<String> dataBuilders = new HashSet<>();
    private final Set<String> executeBefore = new HashSet<>();

    private final Set<Path> tmpFiles = new HashSet<>();

    private Path initScript;

    @Override
    public void includeClassMarker(Class<?> clazz) {
        scriptClasspathMarkerClasses.add(clazz);
    }

    @Override
    public void includeClassMarker(String clazz) {
        scriptClasspathMarkerClasses.add(clazz);
    }

    @Override
    public void includeResourceMarker(String resource) {
        scriptClasspathMarkerResources.add(resource);
    }

    @Override
    public void addDataBuilder(String className) {
        dataBuilders.add(className);
    }

    @Override
    public void executeBefore(String... tasks) {
        Collections.addAll(executeBefore, tasks);
    }

    @Override
    public Set<String> getDataBuilders() {
        return dataBuilders;
    }

    @Override
    public Set<String> getExecuteBefore() {
        return executeBefore;
    }

    @Override
    public Path getInitScript() {
        if (initScript == null) {

            initScript = sneaky(() -> Files.createTempFile("wt_init", ".gradle"));
            tmpFiles.add(initScript);
            logger.info("Building InitScript..");
            //TODO, cache.
            //Compute our additions.
            String depLine = Stream.concat(//
                    scriptClasspathMarkerClasses.parallelStream()//
                            .map(GradleManagerImpl::getJarPathForClass),//
                    scriptClasspathMarkerResources.parallelStream()//
                            .map(Utils::getJarPathForResource)//
            ).parallel()//
                    .filter(Objects::nonNull)//
                    .map(p -> {
                        try {
                            if (Files.isDirectory(p)) {
                                String str = p.toString();
                                int idx = str.indexOf("/out/");
                                String prefix = "wt_tmp_jar" + (idx > 0 ? "_" + str.substring(idx + 5).replace("/", "_") : "");
                                Path tmpJar = Files.createTempFile(prefix, ".jar");
                                Files.delete(tmpJar);//ZipFileSystem assumptions.
                                tmpFiles.add(tmpJar);
                                try (FileSystem jarFS = getJarFileSystem(tmpJar, true)) {
                                    Files.walkFileTree(p, new CopyingFileVisitor(p, jarFS.getPath("/")));
                                }
                                return tmpJar;
                            }
                            return p;
                        } catch (IOException e) {
                            throwUnchecked(e);
                            return null;
                        }
                    })//
                    .map(Path::toString)//
                    .map(e -> e.replace("\\", "\\\\"))
                    .map(e -> "'" + e + "'")//
                    .collect(Collectors.joining(", ", "        classpath files([", "])"));

            //Read all lines.
            List<String> scriptLines;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(GradleManagerImpl.class.getResourceAsStream("/templates/gradle/init.gradle")))) {
                scriptLines = reader.lines().collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read template.", e);
            }
            int idx = -1;
            //find our marker line.
            for (int i = 0; i < scriptLines.size(); i++) {
                String line = scriptLines.get(i);
                if (line.contains("$ { DEPENDENCIES }")) {
                    idx = i;
                    break;
                }
            }
            //remove marker and add all dep lines.
            if (idx == -1) {
                throw new RuntimeException("Unable to find Dependencies marker in init script template.");
            }
            scriptLines.remove(idx);
            scriptLines.add(idx, depLine);
            sneaky(() -> Files.write(initScript, scriptLines));
            logger.info("InitScript built to: {}", initScript);
        }
        return initScript;
    }

    private static Path getJarPathForClass(Object obj) {
        if (obj instanceof Class) {
            return Utils.getJarPathForClass((Class) obj);
        } else if (obj instanceof CharSequence) {
            return Utils.getJarPathForClass(obj.toString());
        }
        throw new RuntimeException("Unhandled type: " + obj.getClass());
    }

    @Override
    public void close() throws IOException {
        tmpFiles.forEach(p -> sneaky(() -> Files.delete(p)));
    }
}
