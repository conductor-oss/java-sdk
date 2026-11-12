/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.sdk.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Functions;
import org.conductoross.conductor.sdk.Promise;

/**
 * What workflow code can ask of whatever is running it. The static API in {@code Conductor}
 * forwards every call to the engine bound to the current thread: the replay engine when the code
 * runs against a server, the in-memory engine in tests.
 */
public interface Engine {

    /** A call that needs a result; returns it decoded, or throws TaskFailure. */
    Object call(Command command);

    /** A call that needs no result (events, fire-and-forget workflows). */
    void fire(Command command);

    Object once(String name, Supplier<?> supplier);

    Instant now();

    double random();

    <T> Promise<T> async(Supplier<T> body);

    void join(List<? extends Promise<?>> promises);

    <I, R> List<R> fanOut(String name, List<I> items, Function<I, R> body);

    boolean awaitCondition(BooleanSupplier condition, Duration timeout);

    Object repeatUntil(Supplier<?> body, Functions.Pred<?> condition, Backoff backoff);

    /** Runs one task per item, sequentially, on the server (a DO_WHILE over the items). */
    List<Object> forEach(String name, List<?> items, Command task, String itemParameter);

    /**
     * A SWITCH on {@code value}: emits the switch with every declared case so the graph shows
     * all branches, runs the body of the case the value selects (or {@code otherwise}), and
     * returns what it returned. Cases are keyed by the value's string form.
     */
    Object switchOn(String name, Object value, java.util.Map<String, Supplier<?>> cases, Supplier<?> otherwise);

    String workflowId();

    // ------------------------------------------------------------------ binding

    /** Throws when the engine is shutting this code down and no call may take effect. */
    default void assertRunning() {}

    ThreadLocal<Engine> CURRENT = new ThreadLocal<>();

    static Engine current() {
        Engine engine = CURRENT.get();
        if (engine == null) {
            throw new IllegalStateException(
                    "this call is only valid inside a @Workflow method run by Conductor");
        }
        engine.assertRunning();
        return engine;
    }
}
