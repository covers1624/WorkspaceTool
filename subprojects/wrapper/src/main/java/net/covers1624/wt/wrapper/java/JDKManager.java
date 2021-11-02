package net.covers1624.wt.wrapper.java;

import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.download.DownloadAction;
import net.covers1624.quack.net.download.DownloadListener;
import net.covers1624.wt.wrapper.json.AdoptiumRelease;
import net.covers1624.wt.wrapper.json.JsonUtils;
import net.rubygrapefruit.platform.internal.Platform;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static net.covers1624.quack.collection.ColUtils.iterable;

/**
 * Created by covers1624 on 30/10/21.
 */
public class JDKManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDKManager.class);
    private static final String ADOPTIUM_URL = "https://api.adoptium.net";
    private static final Platform PLATFORM = Platform.current();
    private static final Type INSTALLED_TYPE = new TypeToken<Map<String, Path>>() { }.getType();

    private final Path jdksDir;
    private final Path installedJson;
    public final Map<String, Path> installed;

    public JDKManager(Path jdksDir) {
        this.jdksDir = jdksDir;
        installedJson = jdksDir.resolve("installed.json");
        if (Files.exists(installedJson)) {
            installed = JsonUtils.parse(installedJson, INSTALLED_TYPE);
        } else {
            installed = new HashMap<>();
        }
    }

    @Nullable
    public Path findJDK(JavaVersion version) {
        for (Map.Entry<String, Path> entry : installed.entrySet()) {
            if (JavaVersion.parse(entry.getKey()) == version) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Path installJdk(JavaVersion version) {
        try {
            List<AdoptiumRelease> releases = getRelease(version);
            if (releases.isEmpty()) throw new RuntimeException("No releases returned.");
            AdoptiumRelease release = releases.get(0);
            if (release.binaries.size() != 1) throw new RuntimeException("wat? api bork?");
            LOGGER.info("Downloading Adoptium JDK: {}", release.release_name);
            AdoptiumRelease.Binary binary = release.binaries.get(0);
            Path dest = jdksDir.resolve(binary._package.name);
            dest.toFile().deleteOnExit();
            DownloadAction action = new DownloadAction();
            action.setSrc(binary._package.link);
            action.setDest(dest);
            action.setListener(new StatusDownloadListener());
            action.execute();
            Path jdkDir = extract(jdksDir, dest);
            installed.put(release.version_data.semver, jdkDir);
            JsonUtils.write(installedJson, installed, INSTALLED_TYPE);
            return jdkDir;
        } catch (IOException e) {
            LOGGER.error("Failed to download Adoptium JDK for this platform.", e);
            System.exit(0);
            return null;
        }
    }

    private static List<AdoptiumRelease> getRelease(JavaVersion version) throws IOException {
        Path tempFile = Files.createTempFile("adoptium_release", ".json");
        Architecture architecture = Architecture.current();
        tempFile.toFile().deleteOnExit();
        try {
            DownloadAction action = new DownloadAction();
            action.setDest(tempFile);
            action.setSrc(makeURL(version, architecture));
            action.execute();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 404 || !PLATFORM.isMacOs() || architecture != Architecture.AARCH64) {
                throw e;
            }
            // Try x64.
            DownloadAction action = new DownloadAction();
            action.setDest(tempFile);
            action.setSrc(makeURL(version, Architecture.X64));
            action.execute();
        }
        return AdoptiumRelease.parseReleases(tempFile);
    }

    private static URL makeURL(JavaVersion version, Architecture architecture) {
        String platform;
        if (PLATFORM.isWindows()) {
            platform = "windows";
        } else if (PLATFORM.isLinux()) {
            platform = "linux";
        } else if (PLATFORM.isMacOs()) {
            platform = "mac";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system.");
        }
        try {
            return new URL(ADOPTIUM_URL
                    + "/v3/assets/feature_releases/"
                    + version.shortString
                    + "/ga"
                    + "?project=jdk"
                    + "&image_type=jdk"
                    + "&vendor=eclipse"
                    + "&jvm_impl=hotspot"
                    + "&heap_size=normal"
                    + "&architecture=" + architecture.name().toLowerCase(Locale.ROOT)
                    + "&os=" + platform
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create URL.", e);
        }
    }

    private static Path extract(Path jdksDir, Path jdkArchive) {
        LOGGER.info("Extracting archive: {}", jdkArchive.getFileName());
        try {
            Path jdkDir = jdksDir.resolve(getBasePath(jdkArchive));
            try (ArchiveInputStream is = createStream(jdkArchive)) {
                ArchiveEntry entry;
                while ((entry = is.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    Path file = jdksDir.resolve(entry.getName()).toAbsolutePath();
                    Files.createDirectories(file.getParent());
                    try (OutputStream os = Files.newOutputStream(file)) {
                        IOUtils.copy(is, os);
                    }
                }
            }
            if (PLATFORM.isMacOs() || PLATFORM.isLinux()) {
                makeExecutable(jdkDir.resolve("bin"));
            }
            return jdkDir;
        } catch (IOException e) {
            LOGGER.error("Unable to extract archive. " + jdkArchive, e);
            System.exit(1);
            return null;
        }
    }

    private static String getBasePath(Path jdkArchive) throws IOException {
        try (ArchiveInputStream is = createStream(jdkArchive)) {
            ArchiveEntry entry;
            while ((entry = is.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    return entry.getName();
                }
            }
        }
        throw new RuntimeException("Unable to find base path for archive. " + jdkArchive);
    }

    private static ArchiveInputStream createStream(Path jdkArchive) throws IOException {
        String fileName = jdkArchive.getFileName().toString();
        if (fileName.endsWith(".tar.gz")) {
            return new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(jdkArchive)));
        }
        if (fileName.endsWith(".zip")) {
            return new ZipArchiveInputStream(Files.newInputStream(jdkArchive));
        }
        throw new UnsupportedOperationException("Unable to determine archive format of file: " + fileName);
    }

    private static void makeExecutable(Path binFolder) throws IOException {
        for (Path path : iterable(Files.list(binFolder))) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxr-x"));
        }
    }

    private static class StatusDownloadListener implements DownloadListener {

        int lastLen = 0;
        private long expected;

        @Override
        public void connecting() {
            System.out.print("Connecting..");
        }

        @Override
        public void start(long expectedLen) {
            expected = expectedLen;
        }

        @Override
        public void update(long processedBytes) {
            String line = "Downloading... (" + getStatus(processedBytes, expected) + ")";
            lastLen = line.length();
            System.out.print("\r" + line);
        }

        @Override
        public void finish(long totalProcessed) {
            System.out.print("\r" + StringUtils.repeat(' ', lastLen) + "\r");
        }

        private String getStatus(long complete, long total) {
            if (total >= 1024) return toKB(complete) + "/" + toKB(total) + " KB";
            if (total >= 0) return complete + "/" + total + " B";
            if (complete >= 1024) return toKB(complete) + " KB";
            return complete + " B";
        }

        protected long toKB(long bytes) {
            return (bytes + 1023) / 1024;
        }
    }
}
