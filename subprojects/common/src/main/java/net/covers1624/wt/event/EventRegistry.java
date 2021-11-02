/*
 * This file is part of WorkspaceTool and is Licensed under the MIT License.
 *
 * Copyright (c) 2018-2021 covers1624 <https://github.com/covers1624>
 */
package net.covers1624.wt.event;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 30/6/19.
 */
public class EventRegistry<E extends Event> {

    private final EventRegistry parent;
    private final Class<E> target;

    @SuppressWarnings ("unchecked")
    private Listener<E>[][] listeners = new Listener[Event.Priority.values().length][0];

    public EventRegistry(Class<E> target) {
        this(null, target);
    }

    public EventRegistry(EventRegistry parent, Class<E> target) {
        this.parent = parent;
        this.target = target;
    }

    /**
     * Registers a Consumer to this EventRegistry.
     * This is an overload of {@link #register(Event.Priority, Consumer)},
     * with the {@link Event.Priority} defaulted to {@link Event.Priority#NORMAL},
     * and 'receiveCanceled' set to false.
     *
     * @param consumer The callback.
     * @return A HandlerKey, used to UnRegister this specific Consumer, See {@link #unRegister(HandlerKey)}
     */
    public HandlerKey register(Consumer<E> consumer) {
        return register(Event.Priority.NORMAL, consumer);
    }

    /**
     * Registers a Consumer to this EventRegistry.
     * Master method, all overloads end here.
     *
     * @param priority The Priority to call this Consumer.
     * @param consumer The callback.
     * @return A HandlerKey, used to UnRegister this specific Consumer, See {@link #unRegister(HandlerKey)}
     */
    public HandlerKey register(Event.Priority priority, Consumer<E> consumer) {
        Listener<E> listener = new Listener<>(consumer, priority, new HandlerKey());
        Listener<E>[] pListeners = listeners[priority.ordinal()];
        listeners[priority.ordinal()] = pListeners = Arrays.copyOf(pListeners, pListeners.length + 1);
        pListeners[pListeners.length - 1] = listener;
        return listener.key;
    }

    /**
     * UnRegisters a specific Consumer from this EventRegistry.
     *
     * @param key The HandlerKey returned by {@link #register(Event.Priority, Consumer)}
     */
    public void unRegister(HandlerKey key) {
        Listener<E>[] listeners = this.listeners[key.listener.priority.ordinal()];
        //Find the index of our key.
        int index = -1;
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i].key.equals(key)) {
                index = i;
                break;
            }
        }
        //kek
        if (index == -1) {
            throw new IllegalArgumentException("That key is not known to this EventRegistry.");
        }
        //Shift all elements to the right of our element, to the left.
        Listener<E>[] tmp = Arrays.copyOf(listeners, listeners.length);
        int n = listeners.length - index - 1;
        if (n > 0) {
            System.arraycopy(tmp, index + 1, tmp, index, n);
        }
        //Trim and assign back.
        this.listeners[key.listener.priority.ordinal()] = Arrays.copyOf(tmp, tmp.length - 1);
    }

    /**
     * Fires the event to all Listeners.
     *
     * @param event The event.
     * @return The same event.
     */
    public E fireEvent(E event) {
        for (Event.Priority priority : Event.Priority.values()) {
            fireInternal(event, priority);
        }
        return event;
    }

    private void fireInternal(E event, Event.Priority priority) {
        for (Listener<E> listener : listeners[priority.ordinal()]) {
            if (event instanceof ResultEvent) {
                ResultEvent rE = (ResultEvent) event;
                if (rE.hasResult() && rE.isGreedy()) {
                    listener.consumer.accept(event);
                } else if (!rE.hasResult()) {
                    listener.consumer.accept(event);
                }
            } else {
                listener.consumer.accept(event);
            }
        }
        if (parent != null) {
            //noinspection unchecked
            parent.fireInternal(event, priority);
        }
    }

    private static class Listener<E> {

        final Consumer<E> consumer;
        final Event.Priority priority;
        final HandlerKey key;

        private Listener(Consumer<E> consumer, Event.Priority priority, HandlerKey key) {
            this.consumer = consumer;
            this.priority = priority;
            this.key = key;
            key.listener = this;
        }
    }

    public static final class HandlerKey {

        private static final AtomicInteger counter = new AtomicInteger();

        private final int inc = counter.getAndIncrement();

        Listener listener;

        private HandlerKey() {
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof HandlerKey) {
                return ((HandlerKey) obj).inc == inc;
            }
            return false;
        }
    }

}
