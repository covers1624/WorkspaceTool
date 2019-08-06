package net.covers1624.wt.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Isolates the specified packages away from its parent.
 * Useful for _somewhat_ sand boxing stuff.
 *
 * Created by covers1624 on 9/04/19.
 */
public class IsolatingClassLoader extends URLClassLoader {

    private final ClassLoader parent;
    private final Set<String> packages;
    private BiFunction<ClassLoader, String, Class> parentLookup;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatingClassLoader() {
        this(IsolatingClassLoader.class.getClassLoader());
    }

    public IsolatingClassLoader(ClassLoader parent) {
        super(new URL[] {}, parent);
        this.parent = parent;
        packages = new HashSet<>();
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

    /**
     * Add a package that this ClassLoader will always load instead of the parent.
     *
     * @param pkg The package.
     * @return The same IsolatingClassLoader.
     */
    public IsolatingClassLoader addPackage(String pkg) {
        packages.add(pkg);
        return this;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
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
            if (packages.stream().anyMatch(name::startsWith)) {
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
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (packages.stream().anyMatch(name::startsWith)) {
            String rName = name.replace(".", "/") + ".class";
            URL url = getResource(rName);
            if (url != null) {
                try {
                    return load(name, rName, url);
                } catch (IOException ignored) {

                }
            }
        }
        return super.findClass(name);
    }

    private Class<?> load(String name, String rName, URL url) throws IOException {
        int lastDot = name.lastIndexOf(".");
        String pkgName = lastDot > -1 ? name.substring(0, lastDot) : "";
        URLConnection urlConnection = url.openConnection();
        CodeSigner[] signers = null;
        if (urlConnection instanceof JarURLConnection) {
            JarURLConnection juc = (JarURLConnection) urlConnection;
            JarFile jf = juc.getJarFile();
            if (jf != null && jf.getManifest() != null) {
                Manifest manifest = jf.getManifest();
                JarEntry je = jf.getJarEntry(rName);

                Package pkg = getPackage(pkgName);
                signers = je.getCodeSigners();
                if (pkg == null) {
                    definePackage(pkgName, manifest, juc.getJarFileURL());
                }
            }
        } else {
            Package pkg = getPackage(pkgName);
            if (pkg == null) {
                definePackage(pkgName, null, null, null, null, null, null, null);
            }
        }
        byte[] bytes;
        try (InputStream is = urlConnection.getInputStream()) {
            bytes = Main.toBytes(is);
        }
        CodeSource cs = urlConnection != null ? new CodeSource(urlConnection.getURL(), signers) : null;
        return defineClass(name, bytes, 0, bytes.length, cs);
    }

}
