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
package org.conductoross.conductor.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.conductoross.conductor.sdk.internal.Replayer;

/**
 * The executions whose code this process keeps in memory between continuations.
 *
 * <p>After a continuation the lanes of the code are left parked where they paused, on their
 * virtual threads, instead of being torn down. When the execution's next continuation comes to
 * this process, its lane is woken with the result of the task it waited for and nothing is run
 * again: no replay, and no fetch of the execution's history.
 *
 * <p>It is a cache and nothing else. The server's recording remains what the execution is, so an
 * execution dropped from here (it sat idle too long, too many are held, the process restarted) is
 * replayed by whichever worker gets its next continuation. An execution is out of the cache while
 * one of its continuations runs ({@link #take}), so only one thread ever has its lanes.
 */
final class ParkedExecutions {

    /**
     * One execution's lanes, parked.
     *
     * @param stamp what the completion of its last continuation carried; the lanes may continue
     *     only while the server still names it as the last completion that applied
     * @param until when it has sat idle for too long, in the clock's milliseconds
     */
    record Parked(Replayer replayer, String stamp, long until) {}

    /** By code type, each in the order they were parked: the first is the one idle longest. */
    private final Map<String, LinkedHashMap<String, Parked>> byCodeType = new HashMap<>();
    private final LongSupplier clock;
    private final AtomicLong continued = new AtomicLong();
    private final AtomicLong replayed = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();

    ParkedExecutions() {
        this(System::currentTimeMillis);
    }

    ParkedExecutions(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Takes an execution's parked lanes out, for the continuation about to run; null when none
     * are held. The caller parks them again, or closes them.
     */
    synchronized Parked take(String codeType, String workflowId) {
        LinkedHashMap<String, Parked> held = byCodeType.get(codeType);
        return held == null ? null : held.remove(workflowId);
    }

    /**
     * Keeps an execution's lanes until its next continuation, for at most {@code idleMillis}. When
     * that makes more than {@code max} of its code type, the ones idle longest are closed.
     */
    void park(String codeType, String workflowId, Replayer replayer, String stamp, long idleMillis, int max) {
        List<Parked> closing = new ArrayList<>();
        synchronized (this) {
            LinkedHashMap<String, Parked> held = byCodeType.computeIfAbsent(codeType, k -> new LinkedHashMap<>());
            Parked replaced = held.put(workflowId, new Parked(replayer, stamp, clock.getAsLong() + idleMillis));
            if (replaced != null) {
                closing.add(replaced); // two workers threads were handed the same continuation
            }
            for (Iterator<Parked> eldest = held.values().iterator(); held.size() > Math.max(1, max); ) {
                closing.add(eldest.next());
                eldest.remove();
            }
        }
        close(closing);
    }

    /** How many executions of a code type are held. */
    synchronized int held(String codeType) {
        LinkedHashMap<String, Parked> held = byCodeType.get(codeType);
        return held == null ? 0 : held.size();
    }

    /** Closes the executions that have sat idle past their time. */
    void dropIdle() {
        List<Parked> closing = new ArrayList<>();
        long now = clock.getAsLong();
        synchronized (this) {
            for (LinkedHashMap<String, Parked> held : byCodeType.values()) {
                held.values().removeIf(p -> {
                    if (p.until() > now) {
                        return false;
                    }
                    closing.add(p);
                    return true;
                });
            }
        }
        close(closing);
    }

    /** Closes every execution held: the process stops hosting. */
    void closeAll() {
        List<Parked> closing = new ArrayList<>();
        synchronized (this) {
            byCodeType.values().forEach(held -> closing.addAll(held.values()));
            byCodeType.clear();
        }
        closing.forEach(p -> p.replayer().close());
    }

    /** Outside the lock: closing a replayer waits for its lanes' threads to unwind. */
    private void close(List<Parked> closing) {
        for (Parked p : closing) {
            dropped.incrementAndGet();
            p.replayer().close();
        }
    }

    void countContinued() {
        continued.incrementAndGet();
    }

    void countReplayed() {
        replayed.incrementAndGet();
    }

    /** Continuations that continued parked lanes, with no replay. */
    long continued() {
        return continued.get();
    }

    /** Continuations that replayed the execution: nothing was held, or it was no longer current. */
    long replayed() {
        return replayed.get();
    }

    /** Executions closed while held: idle too long, too many, or replaced. */
    long dropped() {
        return dropped.get();
    }
}
