/*
 * Copyright 2025 Conductor Authors.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.ConductorClientEvent;
import com.netflix.conductor.client.events.taskrunner.PollStarted;

import static org.junit.jupiter.api.Assertions.*;

class EventDispatcherTest {

    private EventDispatcher<ConductorClientEvent> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new EventDispatcher<>();
    }

    @Test
    void testRegisterAndPublish() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PollStarted> received = new AtomicReference<>();

        Consumer<PollStarted> listener = event -> {
            received.set(event);
            latch.countDown();
        };

        dispatcher.register(PollStarted.class, listener);
        PollStarted event = new PollStarted("testTask");
        dispatcher.publish(event);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Listener should have been called within 1 second");
        assertSame(event, received.get());
        assertEquals("testTask", received.get().getTaskType());
    }

    @Test
    void testUnregister() throws InterruptedException {
        AtomicBoolean called = new AtomicBoolean(false);

        Consumer<PollStarted> listener = event -> called.set(true);

        dispatcher.register(PollStarted.class, listener);
        dispatcher.unregister(PollStarted.class, listener);
        dispatcher.publish(new PollStarted("testTask"));

        // Give async execution a chance to run (if it were to fire)
        Thread.sleep(200);
        assertFalse(called.get(), "Listener should not have been called after unregistration");
    }

    @Test
    void testPublishNoListeners() {
        // Should not throw NPE or any exception when no listeners are registered
        assertDoesNotThrow(() -> dispatcher.publish(new PollStarted("orphanTask")));
    }

    @Test
    void testPromiscuousListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ConductorClientEvent> received = new AtomicReference<>();

        Consumer<ConductorClientEvent> promiscuousListener = event -> {
            received.set(event);
            latch.countDown();
        };

        dispatcher.register(ConductorClientEvent.class, promiscuousListener);
        PollStarted event = new PollStarted("anyTask");
        dispatcher.publish(event);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Promiscuous listener should receive all event types");
        assertSame(event, received.get());
        assertInstanceOf(PollStarted.class, received.get());
    }

    @Test
    void testMultipleListeners() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Consumer<PollStarted> listener1 = event -> latch.countDown();
        Consumer<PollStarted> listener2 = event -> latch.countDown();

        dispatcher.register(PollStarted.class, listener1);
        dispatcher.register(PollStarted.class, listener2);
        dispatcher.publish(new PollStarted("multiTask"));

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Both listeners should have been called");
    }

    @Test
    void testUnregisterFromEmpty() {
        Consumer<PollStarted> listener = event -> {};

        // Unregister from a type that was never registered - should not throw
        assertDoesNotThrow(() -> dispatcher.unregister(PollStarted.class, listener));
    }
}
