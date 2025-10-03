/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wstool.wrapper;

import com.google.gson.Gson;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.HttpEngineDownloadAction;
import net.covers1624.quack.net.httpapi.HttpEngine;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 9/11/21.
 */
public final class RuntimeResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeResolver.class);
    private static final Gson GSON = new Gson();
    private static final MetadataXpp3Reader METADATA_READER = new MetadataXpp3Reader();

    private static final Path MAVEN_LOCAL = Paths.get(System.getProperty("user.home")).resolve(".m2/repository");

    private final HttpEngine http;
    private final WrapperProperties properties;
    private final Path librariesDir;

    public RuntimeResolver(HttpEngine http, WrapperProperties properties, Path librariesDir) {
        this.http = http;
        this.librariesDir = librariesDir;
        this.properties = properties;
    }

    public @Nullable RuntimeEnvironment resolve(boolean useLocal) throws IOException {
        ResolvedManifest resolved = useLocal ? getManifestForLocal() : getManifestForRemote();
        if (resolved == null) return null;
        String version = resolved.notation.version;
        WrapperManifest manifest = resolved.manifest;
        try {
            if (!VersionRange.createFromVersionSpec(manifest.wrapperVersionFilter()).containsVersion(new DefaultArtifactVersion(Main.VERSION))) {
                LOGGER.error("Resolved WorkspaceTool version {} requires a wrapper matching {}. Current wrapper version {}. Please update your wrapper.", version, manifest.wrapperVersionFilter(), Main.VERSION);
                return null;
            }
        } catch (InvalidVersionSpecificationException ex) {
            throw new RuntimeException(ex);
        }

        LOGGER.info("Resolving runtime..");
        Map<String, String> replacements = new HashMap<>();
        for (Map.Entry<String, List<WrapperManifest.Dependency>> entry : manifest.classPaths().entrySet()) {
            List<Path> dependencies = new ArrayList<>();
            for (WrapperManifest.Dependency dependency : entry.getValue()) {
                dependencies.add(downloadFile(dependency.artifact(), dependency.sha256(), dependency.size()));
            }
            String str = FastStream.of(dependencies)
                    .map(e -> e.toAbsolutePath().toString())
                    .join(File.pathSeparator);
            replacements.put("classPaths." + entry.getKey(), str);
        }

        return new RuntimeEnvironment(
                manifest.javaVersion(),
                FastStream.of(manifest.javaArgs())
                        .map(e -> replacePatterns(replacements, e))
                        .toList()
        );
    }

    private @Nullable ResolvedManifest getManifestForLocal() throws IOException {
        String resolved = getLocalVersion(properties.artifact);
        if (resolved == null) {
            LOGGER.error("Unable to find a published local version.");
            return null;
        }

        MavenNotation artifact = properties.artifact.withVersion(resolved);

        Path manifestPath = artifact.withExtension("json")
                .toPath(MAVEN_LOCAL);
        WrapperManifest manifest = JsonUtils.parse(GSON, manifestPath, WrapperManifest.class);
        return new ResolvedManifest(artifact, manifest);
    }

    private @Nullable ResolvedManifest getManifestForRemote() throws IOException {
        String resolved = getVersion(properties.artifact);
        if (resolved == null) {
            LOGGER.error("Unable to find version {}.", properties.artifact);
            return null;
        }

        MavenNotation artifact = properties.artifact.withVersion(resolved);
        Path manifestPath = downloadFile(
                artifact.withExtension("json"),
                null,
                -1
        );

        Path manifestSignaturePath = downloadFile(
                artifact.withExtension("json.asc"),
                null,
                -1
        );
        if (!validateSignature(manifestPath, manifestSignaturePath)) {
            LOGGER.error("Could not validate authenticity of wrapper launch manifest.");
            return null;
        }

        WrapperManifest manifest = JsonUtils.parse(GSON, manifestPath, WrapperManifest.class);
        return new ResolvedManifest(artifact, manifest);
    }

    private String replacePatterns(Map<String, String> replacements, String str) {
        int start = str.indexOf("${");
        if (start == -1) return str;

        int end = str.indexOf("}", start);
        if (end == -1) {
            throw new IllegalArgumentException("Unbalanced braces. " + str);
        }

        String pattern = str.substring(start + 2, end);
        String repl = replacements.get(pattern);
        if (repl == null) throw new IllegalArgumentException("Unknown pattern. " + pattern);

        return replacePatterns(replacements, str.substring(0, start) + repl + str.substring(end + 1));
    }

    private @Nullable String getVersion(MavenNotation notation) throws IOException {
        requireNonNull(notation.version);

        Path metaFile = downloadFile(
                new URL(properties.mirror + notation.toModulePath() + "maven-metadata.xml"),
                librariesDir.resolve(notation.toModulePath() + "maven-metadata.xml"),
                null,
                -1
        );

        return resolveVersion(parseMavenMetadata(metaFile), notation.version);
    }

    private @Nullable String getLocalVersion(MavenNotation notation) throws IOException {
        requireNonNull(notation.version);

        Path file = MAVEN_LOCAL.resolve(notation.toModulePath()).resolve("maven-metadata-local.xml");
        if (Files.notExists(file)) return null;

        return resolveVersion(parseMavenMetadata(file), notation.version);
    }

    private Path downloadFile(MavenNotation notation, @Nullable String sha256, int expectedLen) throws IOException {
        Path dest = notation.toPath(librariesDir);
        Path localFile = notation.toPath(MAVEN_LOCAL);
        if (Files.exists(localFile) && validate(localFile, expectedLen, sha256)) {
            if (Files.notExists(dest) || !validate(dest, expectedLen, sha256)) {
                LOGGER.info("Found file {} locally.", notation);
                Files.copy(localFile, IOUtils.makeParents(dest), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return downloadFile(
                notation.toURL(properties.mirror),
                dest,
                sha256,
                expectedLen
        );
    }

    private Path downloadFile(URL url, Path dest, @Nullable String sha256, int expectedLen) throws IOException {
        if (Files.exists(dest) && sha256 != null && validate(dest, expectedLen, sha256)) {
            return dest;
        }
        LOGGER.info("Downloading file {}.", url);
        IOException exception = null;
        for (int i = 0; i < 10; i++) {
            try {
                new HttpEngineDownloadAction(http)
                        .setUrl(url.toString())
                        .setDest(dest)
                        .setDownloadListener(new StatusDownloadListener())
                        .setUseETag(true)
                        .execute();
                if (!validate(dest, expectedLen, sha256)) {
                    LOGGER.error("Download validations failed. File will be re-downloaded.");
                    continue;
                }
                LOGGER.info(" Downloaded from {}", url);
                exception = null;
                break;
            } catch (IOException ex) {
                if (exception == null) {
                    exception = ex;
                } else {
                    exception.addSuppressed(ex);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return dest;
    }

    private static boolean validate(Path file, long length, @Nullable String sha256) throws IOException {
        if (length != -1) {
            if (Files.size(file) != length) return false;
        }

        if (sha256 != null) {
            return Hashing.hashFile(Hashing.SHA256, file).equals(sha256);
        }

        return true;
    }

    private static boolean validateSignature(Path file, Path signature) throws IOException {
        PGPPublicKeyRingCollection collection = loadPublicKey();
        PGPSignature sig = loadSignature(signature);

        PGPPublicKey pubKey = collection.getPublicKey(sig.getKeyID());
        if (pubKey == null) {
            throw new RuntimeException("Unable to verify " + file + ", signature uses untrusted key " + sig.getKeyID());
        }

        try (InputStream is = Files.newInputStream(file)) {
            sig.init(new BcPGPContentVerifierBuilderProvider(), pubKey);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                sig.update(buf, 0, len);
            }

            return sig.verify();
        } catch (PGPException ex) {
            throw new IOException("Failed to verify file.", ex);
        }
    }

    private static PGPPublicKeyRingCollection loadPublicKey() throws IOException {
        try (InputStream is = new ArmoredInputStream(RuntimeResolver.class.getResourceAsStream("/pgp_public_key.asc"))) {
            return new PGPPublicKeyRingCollection(
                    is,
                    new BcKeyFingerprintCalculator()
            );
        } catch (PGPException ex) {
            throw new IOException("Failed to read public key.", ex);
        }
    }

    private static PGPSignature loadSignature(Path sigFile) throws IOException {
        try (BCPGInputStream is = BCPGInputStream.wrap(new ArmoredInputStream(Files.newInputStream(sigFile)))) {
            PGPObjectFactory factory = new PGPObjectFactory(is, new BcKeyFingerprintCalculator());
            Object first = factory.nextObject();

            PGPSignatureList sigList;
            if (first instanceof PGPSignatureList) {
                sigList = (PGPSignatureList) first;
            } else {
                PGPCompressedData compressed = (PGPCompressedData) first;
                sigList = (PGPSignatureList) new PGPObjectFactory(compressed.getDataStream(), new BcKeyFingerprintCalculator())
                        .nextObject();
            }
            if (sigList.isEmpty()) {
                throw new IOException("No signatures found in " + sigFile);
            }

            return sigList.get(0);
        } catch (PGPException ex) {
            throw new IOException("Failed to read signature.", ex);
        }
    }

    private static Metadata parseMavenMetadata(Path metadata) throws IOException {
        try (InputStream is = Files.newInputStream(metadata)) {
            return METADATA_READER.read(is);
        } catch (IOException | XmlPullParserException ex) {
            throw new IOException("Failed to parse maven-metadata.xml", ex);
        }
    }

    @Nullable
    private String resolveVersion(Metadata metadata, String versionFilter) {
        boolean unbounded = versionFilter.endsWith("+");
        String vFilter = unbounded ? versionFilter.substring(0, versionFilter.length() - 1) : versionFilter;
        return FastStream.of(metadata.getVersioning().getVersions())
                .filter(e -> unbounded ? e.startsWith(vFilter) : e.equals(vFilter))
                .sorted(Comparator.comparing(ComparableVersion::new).reversed())
                .firstOrDefault();
    }

    public static class RuntimeEnvironment {

        public final JavaVersion javaVersion;
        public final List<String> javaArgs;

        public RuntimeEnvironment(JavaVersion javaVersion, List<String> javaArgs) {
            this.javaVersion = javaVersion;
            this.javaArgs = javaArgs;
        }
    }

    private static class ResolvedManifest {
        public final MavenNotation notation;
        public final WrapperManifest manifest;

        private ResolvedManifest(MavenNotation notation, WrapperManifest manifest) {
            this.notation = notation;
            this.manifest = manifest;
        }
    }
}
