package net.covers1624.wt.forge.remap;

import com.google.gson.reflect.TypeToken;
import net.covers1624.wt.api.dependency.MavenDependency;
import net.covers1624.wt.api.impl.dependency.MavenDependencyImpl;
import net.covers1624.wt.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by covers1624 on 26/7/19.
 */
public class DependencyRemapper {

    private static final Logger logger = LogManager.getLogger("DependencyRemapper");
    private static final java.lang.reflect.Type gsonType = new TypeToken<Map<String, RemappedData>>() {}.getType();

    private final Path cacheDir;
    private final Path cacheFile;
    private final JarRemapper remapper;
    private Map<String, RemappedData> remapCache = new HashMap<>();

    public DependencyRemapper(Path cacheDir, JarRemapper remapper) {
        this.cacheDir = cacheDir.resolve("remapped_deps");
        cacheFile = this.cacheDir.resolve("remap_cache.json");
        this.remapper = remapper;
        if (Files.exists(cacheFile)) {
            remapCache = Utils.fromJson(cacheFile, gsonType);
        }
    }

    public MavenDependency process(MavenDependency dep) {
        RemappedData remappedData = remapCache.get(dep.getNotation().toString());
        if (remappedData == null) {
            remappedData = new RemappedData();
        }
        if (remappedData.classes == null || !remappedData.classes.exists()) {
            remappedData.classes = cacheDir.resolve(dep.getNotation().toPath()).toFile();
            remapper.process(dep.getClasses(), remappedData.classes.toPath());
            remapCache.put(dep.getNotation().toString(), remappedData);
            save();
        }
        return new RemappedMavenDependency(dep, remappedData);
    }

    private void save() {
        Utils.toJson(remapCache, gsonType, cacheFile);
    }

    private static class RemappedData {

        public File classes;
        public File sources;
        public File javadoc;
    }

    public static class RemappedMavenDependency extends MavenDependencyImpl {

        private final RemappedData data;

        public RemappedMavenDependency(MavenDependency other, RemappedData data) {
            super(other);

            this.data = data;
        }

        @Override
        public Path getClasses() {
            if (data.classes != null) {
                return data.classes.toPath();
            }
            return super.getClasses();
        }

        @Override
        public Path getJavadoc() {
            if (data.javadoc != null) {
                return data.javadoc.toPath();
            }
            return super.getJavadoc();
        }

        @Override
        public Path getSources() {
            if (data.sources != null) {
                return data.sources.toPath();
            }
            return super.getSources();
        }
    }

}
