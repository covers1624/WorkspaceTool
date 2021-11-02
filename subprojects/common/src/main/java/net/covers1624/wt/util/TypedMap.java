/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.covers1624.wt.util.Utils.unsafeCast;

/**
 * Created by covers1624 on 12/8/19.
 */
public class TypedMap implements Map<Object, Object> {

    private final Map<Object, Object> delegate;

    public TypedMap() {
        this(new HashMap<>());
    }

    public TypedMap(Map<Object, Object> delegate) {
        this.delegate = delegate;
    }

    public <T> T put(Key<T> key, T value) {
        return unsafeCast(delegate.put(key, value));
    }

    public <T> T get(Key<T> key) {
        return unsafeCast(delegate.get(key));
    }

    //@formatter:off
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean containsKey(Object key) { return delegate.containsKey(key); }
    @Override public boolean containsValue(Object value) { return delegate.containsValue(value); }
    @Override public Object get(Object key) { return delegate.get(key); }
    @Nullable @Override public Object put(Object key, Object value) { return delegate.put(key, value); }
    @Override public Object remove(Object key) { return delegate.remove(key); }
    @Override public void putAll(@NotNull Map<?, ?> m) { delegate.putAll(m); }
    @Override public void clear() { delegate.clear(); }
    @NotNull @Override public Set<Object> keySet() { return delegate.keySet(); }
    @NotNull @Override public Collection<Object> values() { return delegate.values(); }
    @NotNull @Override public Set<Entry<Object, Object>> entrySet() { return delegate.entrySet(); }
    //@formatter:on

    public static class Key<T> {

        private String name;

        public Key(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            return other.name.equals(name);
        }

        @Override
        public int hashCode() {
            int i = 0;
            i = 31 * i + name.hashCode();
            return i;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
