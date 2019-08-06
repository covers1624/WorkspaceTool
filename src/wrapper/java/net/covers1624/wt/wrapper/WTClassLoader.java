package net.covers1624.wt.wrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.BiFunction;

/**
 * Created by covers1624 on 3/8/19.
 */
public class WTClassLoader extends URLClassLoader {

    private final ClassLoader parent;
    private BiFunction<ClassLoader, String, Class> parentLookup;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public WTClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.parent = parent;
        reflect();
    }

    private void reflect() {
        try {
            Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            parentLookup = (obj, args) -> {
                try {
                    return (Class) m.invoke(obj, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        } else {
            c = parentLookup.apply(parent, name);
            if (c != null) {
                return c;
            }
            try {
                c = findClass(name);
            } catch (ClassNotFoundException ignored) {
            }
            if (c != null) {
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
