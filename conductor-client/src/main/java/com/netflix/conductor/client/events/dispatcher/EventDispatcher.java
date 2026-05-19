/*
 * Copyright 2024 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.client.events.dispatcher;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.netflix.conductor.client.events.ConductorClientEvent;

public class EventDispatcher<T extends ConductorClientEvent> {

    private final Map<Class<? extends T>, ConcurrentHashMap<Object, Consumer<? extends T>>> listeners;

    public EventDispatcher() {
        this.listeners = new ConcurrentHashMap<>();
    }

    /**
     * Register a listener for a specific event type. The consumer itself is
     * used as the registration key, so registering the same consumer reference
     * twice is a no-op.
     */
    public <U extends T> void register(Class<U> clazz, Consumer<U> listener) {
        listeners.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).putIfAbsent(listener, listener);
    }

    /**
     * Register a listener for a specific event type under an explicit key.
     * Subsequent calls with the same {@code (clazz, key)} pair are no-ops,
     * making registration idempotent without external synchronization.
     */
    public <U extends T> void register(Class<U> clazz, Object key, Consumer<U> listener) {
        listeners.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).putIfAbsent(key, listener);
    }

    public <U extends T> void unregister(Class<U> clazz, Consumer<U> listener) {
        var map = listeners.get(clazz);
        if (map != null) {
            map.remove(listener);
            if (map.isEmpty()) {
                listeners.remove(clazz);
            }
        }
    }

    public <U extends T> void unregister(Class<U> clazz, Object key) {
        var map = listeners.get(clazz);
        if (map != null) {
            map.remove(key);
            if (map.isEmpty()) {
                listeners.remove(clazz);
            }
        }
    }

    public void publish(T event) {
        if (noListeners(event)) {
            return;
        }

        CompletableFuture.runAsync(() -> dispatchToListeners(event));
    }

    /**
     * Dispatches {@code event} to registered listeners on the <em>calling</em>
     * thread. Use this instead of {@link #publish(ConductorClientEvent)} when
     * the caller cannot tolerate heap allocation or thread-pool submission
     * (e.g. inside an {@link Thread.UncaughtExceptionHandler} where the JVM
     * may be in an unstable state).
     */
    public void publishSync(T event) {
        if (noListeners(event)) {
            return;
        }

        dispatchToListeners(event);
    }

    @SuppressWarnings("unchecked")
    private void dispatchToListeners(T event) {
        Collection<Consumer<? extends T>> eventListeners = getEventListeners(event);
        for (Consumer<? extends T> listener : eventListeners) {
            ((Consumer<T>) listener).accept(event);
        }
    }

    private boolean noListeners(T event) {
        if (listeners.isEmpty()) {
            return true;
        }

        var specificEventListeners = listeners.get(event.getClass());
        var promiscuousListeners = listeners.get(ConductorClientEvent.class);

        return (specificEventListeners == null || specificEventListeners.isEmpty())
                && (promiscuousListeners == null || promiscuousListeners.isEmpty());
    }

    private Collection<Consumer<? extends T>> getEventListeners(T event) {
        var specificEventListeners = listeners.get(event.getClass());
        var promiscuousListeners = listeners.get(ConductorClientEvent.class);
        if (promiscuousListeners == null || promiscuousListeners.isEmpty()) {
            return specificEventListeners.values();
        }

        if (specificEventListeners == null || specificEventListeners.isEmpty()) {
            return promiscuousListeners.values();
        }

        return Stream.concat(specificEventListeners.values().stream(), promiscuousListeners.values().stream())
                .collect(Collectors.toList());
    }

}
