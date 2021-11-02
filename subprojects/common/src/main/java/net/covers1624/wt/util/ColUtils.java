/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Various Collection Utilities for Iterables and Arrays.
 *
 * Created by covers1624 on 5/08/18.
 */
public class ColUtils {

    public static String toString(Iterable<?> col) {
        return mkString(col, "[ ", ", ", " ]", ColUtils::toString_);
    }

    public static String toString(Object[] col) {
        return mkString(col, "[ ", ", ", " ]", ColUtils::toString_);
    }

    private static String toString_(Object obj) {
        if (obj instanceof Iterable) {
            return toString((Iterable<?>) obj);
        } else if (obj instanceof Object[]) {
            return toString((Object[]) obj);
        } else {
            return String.valueOf(obj);
        }
    }

    public static String mkString(String[] array) {
        return mkString(array, "");
    }

    public static String mkString(String[] array, String sep) {
        return mkString(array, "", sep, "");
    }

    public static String mkString(String[] array, String start, String sep, String end) {
        return mkString(Arrays.asList(array), start, sep, end);
    }

    public static String mkString(Iterable<String> col) {
        return mkString(col, "");
    }

    public static String mkString(Iterable<String> col, String sep) {
        return mkString(col, "", sep, "");
    }

    public static String mkString(Iterable<String> col, String start, String sep, String end) {
        StringBuilder builder = new StringBuilder(start);
        boolean isFirst = true;
        for (String s : col) {
            if (!isFirst) {
                builder.append(sep);
            }
            isFirst = false;
            builder.append(s);
        }
        builder.append(end);
        return builder.toString();
    }

    public static String mkString(Object[] col, String start, String sep, String end, Function<Object, String> func) {
        return mkString(Arrays.asList(col), start, sep, end, func);
    }

    public static String mkString(Iterable<?> col, String start, String sep, String end, Function<Object, String> func) {
        StringBuilder builder = new StringBuilder(start);
        boolean isFirst = true;
        for (Object s : col) {
            if (!isFirst) {
                builder.append(sep);
            }
            isFirst = false;
            builder.append(func.apply(s));
        }
        builder.append(end);
        return builder.toString();
    }

    @SuppressWarnings ("unchecked")
    public static <T> T[] slice(T[] arr, int from, int until) {
        int low = Math.max(from, 0);
        int high = Math.min(Math.max(until, 0), arr.length);
        int size = Math.max(high - low, 0);
        T[] result = (T[]) Array.newInstance(arr.getClass().getComponentType(), size);
        if (size > 0) {
            System.arraycopy(arr, low, result, 0, size);
        }
        return result;
    }

    public static <T> T maxBy(T[] col, ToIntFunction<T> func) {
        return maxBy(Arrays.asList(col), func);
    }

    public static <T> T maxBy(Iterable<T> col, ToIntFunction<T> func) {
        int max = Integer.MIN_VALUE;
        T maxT = null;
        for (T t : col) {
            int x = func.applyAsInt(t);
            if (x > max) {
                maxT = t;
                max = x;
            }
        }
        return maxT;
    }

    public static <T> boolean forAll(T[] col, Predicate<T> func) {
        return forAll(Arrays.asList(col), func);
    }

    public static <T> boolean forAll(Iterable<T> col, Predicate<T> func) {
        for (T t : col) {
            if (!func.test(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean exists(Iterable<T> col, Predicate<T> func) {
        for (T t : col) {
            if (func.test(t)) {
                return true;
            }
        }
        return false;
    }

    public static <T> Optional<T> find(Iterable<T> col, Predicate<T> func) {
        for (T t : col) {
            if (func.test(t)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static <T> Optional<T> headOption(Iterable<T> col) {
        Iterator<T> itr = col.iterator();
        if (itr.hasNext()) {
            return Optional.of(itr.next());
        }
        return Optional.empty();
    }

    public static <T> T head(Iterable<T> col) {
        Iterator<T> itr = col.iterator();
        if (itr.hasNext()) {
            return itr.next();
        }
        throw new RuntimeException("Empty Iterable.");
    }
}
