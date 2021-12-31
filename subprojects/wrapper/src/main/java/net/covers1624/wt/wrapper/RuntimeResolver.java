/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.wrapper;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.download.DownloadAction;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.wt.wrapper.json.RuntimeManifest;
import net.covers1624.wt.wrapper.json.WrapperProperties;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

/**
 * Created by covers1624 on 9/11/21.
 */
@SuppressWarnings ("UnstableApiUsage")
public class RuntimeResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeResolver.class);
    private static final Gson GSON = new Gson();
    private static final MetadataXpp3Reader METADATA_READER = new MetadataXpp3Reader();

    private final Path cacheDir;
    private final String mirror;
    private final MavenNotation artifact;
    private final Path mavenLocal = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    public RuntimeResolver(Path cacheDir, WrapperProperties properties) {
        this.cacheDir = cacheDir;
        mirror = requireNonNull(properties.mirror);
        artifact = requireNonNull(properties.artifact);
    }

    public RuntimeEnvironment resolve(boolean localOnly) throws IOException {
        ComparableVersion resolved = getVersion(artifact, localOnly);
        if (resolved == null) {
            LOGGER.error("Unable to find version {}.", artifact);
            System.exit(-1);
            return null;
        }
        List<String> repos = Arrays.asList(mavenLocal.toUri().toString(), mirror);
        MavenNotation notation = artifact.withVersion(resolved.toString())
                .withExtension("json");
        Path manifestPath = downloadFile(repos, cacheDir, notation, null, -1, true);
        RuntimeManifest manifest = JsonUtils.parse(GSON, manifestPath, RuntimeManifest.class);
        List<Path> dependencies = manifest.dependencies.stream()
                .map(e -> downloadFile(repos, cacheDir, e.artifact, HashCode.fromString(e.sha256), e.size, true))
                .collect(Collectors.toList());

        return new RuntimeEnvironment(manifest.mainClass, manifest.javaVersion, dependencies);
    }

    @Nullable
    private ComparableVersion getVersion(MavenNotation notation, boolean localOnly) {
        assert notation.version != null;

        Metadata localMetadata = parse(mavenLocal.resolve(notation.toModulePath() + "maven-metadata-local.xml"));

        ComparableVersion localVersion = getMaxVersion(localMetadata, notation.version);
        if (localOnly && localVersion != null) return localVersion;

        String metaPath = notation.toModulePath() + "maven-metadata.xml";
        Path metaFile = downloadFile(singletonList(mirror), cacheDir, metaPath, metaPath, null, -1, false);
        Metadata metadata = parse(metaFile);

        ComparableVersion remoteVersion = getMaxVersion(metadata, notation.version);
        if (remoteVersion == null) return localVersion;
        if (localVersion == null) return remoteVersion;
        return remoteVersion.compareTo(localVersion) > 0 ? remoteVersion : localVersion;
    }

    @Nullable
    @Contract ("_,_,_,_,_,true->!null")
    private Path downloadFile(List<String> repos, Path cacheDir, MavenNotation notation, @Nullable HashCode sha256, int expectedLen, boolean fatal) {
        return downloadFile(repos, cacheDir, notation.toPath(), notation.toString(), sha256, expectedLen, fatal);
    }

    @Nullable
    @Contract ("_,_,_,_,_,_,true->!null")
    private Path downloadFile(List<String> repos, Path cacheDir, String path, String desc, @Nullable HashCode sha256, int expectedLen, boolean fatal) {
        Path filePath = cacheDir.resolve(path).toAbsolutePath();
        try {
            if (Files.notExists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (Files.exists(filePath)) {
                if (validate(filePath, sha256, expectedLen)) {
                    return filePath;
                }
                if (sha256 != null) {
                    LOGGER.warn("Failed to validate file {}. The file will be re-acquired.", desc);
                }
            }

            IOException firstException = null;
            outer:
            for (String repo : repos) {
                try {
                    String url = appendIfMissing(repo, "/") + path;
                    LOGGER.info("Trying to download artifact {} from {}.", desc, url);
                    boolean retry = false;
                    while (true) {
                        downloadFile(url, filePath);
                        if (sha256 != null && !validate(filePath, sha256, expectedLen)) {
                            Files.delete(filePath);
                            if (retry) {
                                LOGGER.info("Failed to validate file {}. Trying next repo..", desc);
                                continue outer;
                            }
                            LOGGER.info("Failed to validate file {}. Trying again..", desc);
                            retry = true;
                        } else {
                            break;
                        }
                    }
                    LOGGER.info("Downloaded {}.", desc);
                    return filePath;
                } catch (IOException e) {
                    if (firstException == null) {
                        firstException = e;
                    } else {
                        firstException.addSuppressed(e);
                    }
                }
            }
            if (firstException != null) {
                LOGGER.error("Failed to download file {}.", desc, firstException);
            } else {
                LOGGER.error("Failed to download file {}. No more information is available.", desc);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to download " + desc + ".", e);
        }
        if (fatal) {
            System.exit(-1);
        }
        return null;
    }

    private static boolean validate(Path file, @Nullable HashCode sha256, int expectedLen) throws IOException {
        if (sha256 == null) {
            return false;
        }
        assert expectedLen != -1;

        return Files.size(file) == expectedLen && HashUtils.hash(Hashing.sha256(), file).equals(sha256);
    }

    private static void downloadFile(String url, Path file) throws IOException {
        if (url.startsWith("file://")) {
            Path urlFile = Paths.get(URI.create(url));
            if (Files.notExists(urlFile)) {
                throw new FileNotFoundException(url);
            }
            Files.copy(urlFile, file, StandardCopyOption.REPLACE_EXISTING);
        } else {
            DownloadAction action = new DownloadAction();
            action.setSrc(url);
            action.setDest(file);
            action.setUseETag(true);
            action.setOnlyIfModified(true);
            action.setListener(new StatusDownloadListener());
            action.execute();
        }
    }

    @Nullable
    private static Metadata parse(@Nullable Path metadata) {
        if (metadata != null && Files.exists(metadata)) {
            try (InputStream is = Files.newInputStream(metadata)) {
                return METADATA_READER.read(is);
            } catch (IOException | XmlPullParserException e) {
                LOGGER.warn("Failed to read Metadata xml: " + metadata, e);
            }
        }
        return null;
    }

    @Nullable
    private ComparableVersion getMaxVersion(@Nullable Metadata metadata, String versionFilter) {
        if (metadata == null) return null;
        boolean startsWith = versionFilter.endsWith("+");
        String vFilter = startsWith ? versionFilter.substring(0, versionFilter.length() - 1) : versionFilter;
        return metadata.getVersioning().getVersions().stream()
                .filter(e -> startsWith ? e.startsWith(vFilter) : e.equals(vFilter))
                .map(ComparableVersion::new)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public static class RuntimeEnvironment {

        public final String mainClass;
        public final JavaVersion javaVersion;
        public final List<Path> dependencies;

        public RuntimeEnvironment(String mainClass, JavaVersion javaVersion, List<Path> dependencies) {
            this.mainClass = mainClass;
            this.javaVersion = javaVersion;
            this.dependencies = dependencies;
        }
    }
}
