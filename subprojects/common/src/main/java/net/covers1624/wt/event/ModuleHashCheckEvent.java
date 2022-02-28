/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2022 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.wt.api.event.VersionedClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects extra variables to be considered when checking the up-to-date status of extracted gradle data.
 * <p>
 * Stored in a map, checked in no particular order, don't step on other peoples shit. Be smart.
 * <p>
 * Created by covers1624 on 2/7/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class ModuleHashCheckEvent extends Event {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final EventRegistry<ModuleHashCheckEvent> REGISTRY = new EventRegistry<>(ModuleHashCheckEvent.class);
    private static final ClassVersionCache classVersionCache = new ClassVersionCache();

    private static final HashFunction sha256 = Hashing.sha256();

    private final Path modulePath;
    private final Map<String, HashCode> extraHashes = new HashMap<>();

    public ModuleHashCheckEvent(Path modulePath) {
        this.modulePath = modulePath;
    }

    public Path getModulePath() {
        return modulePath;
    }

    public Map<String, HashCode> getExtraHashes() {
        return extraHashes;
    }

    public void putHashCode(String key, HashCode v) {
        extraHashes.put(key, v);
    }

    public void putByte(String key, byte v) {
        extraHashes.put(key, sha256.newHasher().putByte(v).hash());
    }

    public void putBytes(String key, byte[] bytes) {
        extraHashes.put(key, sha256.newHasher().putBytes(bytes).hash());
    }

    public void putBytes(String key, byte[] bytes, int off, int len) {
        extraHashes.put(key, sha256.newHasher().putBytes(bytes, off, len).hash());
    }

    public void putBytes(String key, ByteBuffer bytes) {
        extraHashes.put(key, sha256.newHasher().putBytes(bytes).hash());
    }

    public void putChar(String key, char v) {
        extraHashes.put(key, sha256.newHasher().putChar(v).hash());
    }

    public void putShort(String key, short v) {
        extraHashes.put(key, sha256.newHasher().putShort(v).hash());
    }

    public void putInt(String key, int v) {
        extraHashes.put(key, sha256.newHasher().putInt(v).hash());
    }

    public void putFloat(String key, float v) {
        extraHashes.put(key, sha256.newHasher().putFloat(v).hash());
    }

    public void putLong(String key, long v) {
        extraHashes.put(key, sha256.newHasher().putLong(v).hash());
    }

    public void putDouble(String key, double v) {
        extraHashes.put(key, sha256.newHasher().putDouble(v).hash());
    }

    public void putBoolean(String key, boolean v) {
        extraHashes.put(key, sha256.newHasher().putBoolean(v).hash());
    }

    public void putUnencodedChars(String key, CharSequence v) {
        extraHashes.put(key, sha256.newHasher().putUnencodedChars(v).hash());
    }

    public void putString(String key, CharSequence v) {
        extraHashes.put(key, sha256.newHasher().putString(v, StandardCharsets.UTF_8).hash());
    }

    public void putString(String key, CharSequence v, Charset charset) {
        extraHashes.put(key, sha256.newHasher().putString(v, charset).hash());
    }

    public void putVersionedClass(Class<?> clazz) {
        classVersionCache.lookup(clazz).addToEvent(this);
    }

    public void putClassBytes(String cName) {
        try (InputStream is = ModuleHashCheckEvent.class.getResourceAsStream("/" + cName.replace(".", "/") + ".class")) {
            Hasher hasher = sha256.newHasher();
            HashUtils.addToHasher(hasher, is);
            extraHashes.put(cName, hasher.hash());
        } catch (IOException e) {
            LOGGER.error("Unable to get bytes of class {}", cName, e);
        }
    }

    private static class ClassVersionCache {

        private final Map<Class<?>, CachedClass> classCache = new HashMap<>();

        public CachedClass lookup(Class<?> clazz) {
            // Not a computeIfAbsent, due to CME.
            CachedClass existing = classCache.get(clazz);
            if (existing == null) {
                existing = new CachedClass(clazz);
                classCache.put(clazz, existing);
            }
            return existing;
        }

        public class CachedClass {

            public final Class<?> clazz;
            public boolean hasVersionedClass;
            public int versionedClassValue;
            public ObjectStreamClass osc;

            public CachedClass superClass;
            public List<CachedClass> interfaces = new ArrayList<>();

            public CachedClass(Class<?> clazz) {
                this.clazz = clazz;
                if (clazz.isAnnotationPresent(VersionedClass.class)) {
                    hasVersionedClass = true;
                    versionedClassValue = clazz.getAnnotation(VersionedClass.class).value();
                }
                osc = ObjectStreamClass.lookup(clazz);
                if (clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())) {
                    superClass = lookup(clazz.getSuperclass());
                }
                for (Class<?> iFace : clazz.getInterfaces()) {
                    interfaces.add(lookup(iFace));
                }
            }

            public void addToEvent(ModuleHashCheckEvent event) {
                if (hasVersionedClass) {
                    event.putInt("extracted_version:" + clazz.getName(), versionedClassValue);
                }
                if (osc != null) {
                    event.putLong("serialVersionId:" + clazz.getName(), osc.getSerialVersionUID());
                }
                if (superClass != null) {
                    superClass.addToEvent(event);
                }
                interfaces.forEach(e -> e.addToEvent(event));
            }
        }
    }
}
