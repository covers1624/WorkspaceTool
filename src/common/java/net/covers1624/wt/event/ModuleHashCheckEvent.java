package net.covers1624.wt.event;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects extra variables to be considered when checking the up-to-date status of extracted gradle data.
 *
 * Stored in a map, checked in no particular order, don't step on other peoples shit. Be smart.
 *
 * Created by covers1624 on 2/7/19.
 */
@SuppressWarnings ("UnstableApiUsage")
public class ModuleHashCheckEvent extends Event {

    public static final EventRegistry<ModuleHashCheckEvent> REGISTRY = new EventRegistry<>(ModuleHashCheckEvent.class);
    private static final VersionedClassExtractor versionExtractor = new VersionedClassExtractor();

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

    //TODO, Lift restrictions on not passing Classes.
    public void putVersionedClass(String className) {
        versionExtractor.extractFrom(className)//
                .object2IntEntrySet()//
                .forEach(e -> putInt("extracted_version:" + e.getKey(), e.getIntValue()));
    }

    //This is disgusting, needed because we can't load most of the classes provided to putVersionedClass.
    //I hate myself for writing this.
    private static class VersionedClassExtractor {

        private final HashMap<String, Object2IntMap<String>> specificCache = new HashMap<>();

        public Object2IntMap<String> extractFrom(String className) {
            String cName = className.replace(".", "/");
            Object2IntMap<String> curr = specificCache.get(className);
            if (curr != null) {
                return curr;
            }
            Object2IntMap<String> versions = new Object2IntArrayMap<>();
            specificCache.put(cName, versions);
            try (InputStream is = ModuleHashCheckEvent.class.getResourceAsStream("/" + cName + ".class")) {
                ClassReader reader = new ClassReader(is);
                AtomicBoolean ignoreSuper = new AtomicBoolean(false);
                reader.accept(new ClassVisitor(Opcodes.ASM7) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        AnnotationVisitor other = super.visitAnnotation(descriptor, visible);
                        if (descriptor.equals("Lnet/covers1624/wt/event/VersionedClass$IgnoreSuper;")) {
                            ignoreSuper.set(true);
                        } else if (descriptor.equals("Lnet/covers1624/wt/event/VersionedClass;")) {
                            return new AnnotationVisitor(Opcodes.ASM7, other) {
                                @Override
                                public void visit(String name, Object value) {
                                    if (name.equals("value")) {
                                        versions.put(cName, (Integer) value);
                                    }
                                    super.visit(name, value);
                                }
                            };
                        }
                        return other;
                    }
                }, ClassReader.SKIP_CODE);
                if (!ignoreSuper.get()) {
                    String c_super = reader.getSuperName();
                    if (!c_super.equals("java/lang/Object")) {
                        versions.putAll(extractFrom(c_super));
                    }
                    for (String iFace : reader.getInterfaces()) {
                        versions.putAll(extractFrom(iFace));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to get bytes for class: " + className, e);
            }
            return versions;
        }
    }
}
