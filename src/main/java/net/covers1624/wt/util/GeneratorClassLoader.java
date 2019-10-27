package net.covers1624.wt.util;

/**
 * Created by covers1624 on 22/04/19.
 */
public class GeneratorClassLoader extends ClassLoader {

    public GeneratorClassLoader() {
        super(GeneratorClassLoader.class.getClassLoader());
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
