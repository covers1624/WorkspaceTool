/*
 * Copyright (c) 2018-2019 covers1624
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package net.covers1624.wt.wrapper;

import net.covers1624.wt.wrapper.iso.Bootstrap;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * Entry point for the wrapper.
 * If the wrapper detects a production environment by attempting to find
 * 'META-INF/libraries.properties', packed libraries will be extracted
 * and a 'properties.json' file loaded either from your local '.workspace_tool' directory
 * or the global one, if the global one does not exist, it will be generated.
 * The wrapper will then load the maven repositories provided in said json and attempt
 * to resolve the artifact also given in the json. After successful resolution,
 * the resolved jars are thrown on the classpath and the main class provided in the json
 * is launched.
 *
 * If dev env is detected (libraries.properties missing), WorkspaceTool.main is just launched.
 *
 * Created by covers1624 on 1/02/19.
 */
public class Main {

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
    private static File wtFolder = new File(System.getProperty("user.home"), ".workspace_tool");

    public static void main(String[] args) throws Throwable {
        System.setProperty("wt.global.dir", wtFolder.getAbsolutePath());
        URL url = Main.class.getResource("/META-INF/libraries.properties");
        if (url != null) {
            File globalProps = new File(wtFolder, "properties.json");
            File localProps = new File(".workspace_tool/properties.json");

            if (!globalProps.exists()) {
                try (InputStream is = Main.class.getResourceAsStream("/default_properties.json")) {
                    try (FileOutputStream fos = new FileOutputStream(makeFile(globalProps))) {
                        copy(is, fos);
                    }
                }
            }

            WrappedClasspath classpath = computeWrappedClasspath(url, localProps.exists() ? localProps : globalProps);
            URL[] cp = classpath.classPath.stream().map(Main::quietToURL).toArray(URL[]::new);
            URLClassLoader classLoader = new URLClassLoader(cp, Main.class.getClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> clazz = classLoader.loadClass(classpath.mainClass);
            clazz.getMethod("main", String[].class).invoke(null, (Object) args);
        } else {
            //This may seem pointless, but we always ensure out ClassLoader is a URLClassLoader.
            WTClassLoader classLoader = new WTClassLoader(new URL[0], Main.class.getClassLoader());
            Class<?> clazz = Class.forName("net.covers1624.wt.WorkspaceTool", true, classLoader);
            clazz.getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }

    @SuppressWarnings ("unchecked")
    public static WrappedClasspath computeWrappedClasspath(URL libs, File propsFile) throws Throwable {
        Properties properties = new Properties();
        try (InputStream is = libs.openStream()) {
            properties.load(is);
        }
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        IsolatingClassLoader isoClassLoader = new IsolatingClassLoader();
        Thread.currentThread().setContextClassLoader(isoClassLoader);
        isoClassLoader.addPackage("net.covers1624.wt.wrapper.iso");
        for (String name : properties.stringPropertyNames()) {
            File file = new File(wtFolder, "wrapper_libs/" + name);
            boolean extract = !file.exists();
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    String computed = hash(fis);
                    if (!computed.equalsIgnoreCase(properties.getProperty(name))) {
                        extract = true;
                    }
                }
            }
            if (extract) {
                try (InputStream is = Main.class.getResourceAsStream("/META-INF/libs/" + name)) {
                    try (FileOutputStream fos = new FileOutputStream(makeFile(file))) {
                        copy(is, fos);
                    }
                }
            }
            isoClassLoader.addURL(file.toURI().toURL());
        }

        try {
            Class<Bootstrap> clazz =//
                    (Class<Bootstrap>) Class.forName("net.covers1624.wt.wrapper.iso.Bootstrap", true, isoClassLoader);
            return (WrappedClasspath) clazz.getMethod("computeClasspath", File.class, File.class).invoke(null, wtFolder, propsFile);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    public static String hash(InputStream is) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[32 * 1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                sha256.update(buffer, 0, len);
            }
            byte[] hash = sha256.digest();
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] toBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(is, output);
            return output.toByteArray();
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while (-1 != (len = is.read(buffer))) {
            os.write(buffer, 0, len);
        }
    }

    public static File makeFile(File file) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        return file;
    }

    private static URL quietToURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
