package net.covers1624.wstool.minecraft;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by covers1624 on 10/25/26.
 */
public class InterfaceInjectionStubGenerator {

    private static final Type MAP_LIST_TYPE = new TypeToken<Map<String, List<String>>>() { }.getType();
    private static final Gson GSON = new Gson();

    public static void generate(Path jar, List<Path> injectionDefs) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(IOUtils.makeParents(jar)))) {
            for (var file : injectionDefs) {
                Map<String, List<String>> def = JsonUtils.parse(GSON, file, MAP_LIST_TYPE, StandardCharsets.UTF_8);
                var classes = FastStream.of(def.values())
                        .flatMap(e -> e)
                        .toList();

                for (var name : classes) {
                    zos.putNextEntry(new ZipEntry(name + ".class"));
                    zos.write(genInterface(name));
                    zos.closeEntry();
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate Interface Injections jar.", ex);
        }
    }

    private static byte[] genInterface(String name) {
        var cw = new ClassWriter(0);
        // TODO we don't currently declare any generic signature. We would likely need to extend
        //      the json format, or do some annoying regex to grab them from sources. We could probably
        //      count the commas in the generic declaration and just emit `<A, B, C>`, however, Intellij
        //      may complain at that..
        cw.visit(V1_8, ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, name, null, "java/lang/Object", null);
        cw.visitSource(name.substring(name.lastIndexOf('/') + 1) + ".java", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
}
