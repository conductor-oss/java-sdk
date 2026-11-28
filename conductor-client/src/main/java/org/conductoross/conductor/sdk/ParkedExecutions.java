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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * one of its continuations runs ({@link #take}), so only one thread ever has its lanes: the thread
 * that removes it from the map, to continue it or to close it.
 */
final class ParkedExecutions {

    /**
     * One execution's lanes, parked.
     *
     * @param stamp what the completion of its last continuation carried; the lanes may continue
     *     only while the server still names it as the last completion that applied
     * @param until when it has sat idle for too long, in the clock's milliseconds
     * @param order when it was parked, against the others: the lowest is the one idle longest
     */
    record Parked(Replayer replayer, String stamp, long until, long order) {}

    /** By code type, then by workflow id. */
    private final Map<String, Map<String, Parked>> byCodeType = new ConcurrentHashMap<>();
    private final AtomicLong parks = new AtomicLong();
    private final LongSupplier clock;
    /** Continuations that continued parked lanes, with no replay. */
    final AtomicLong continued = new AtomicLong();
    /** Continuations that replayed the execution: nothing was held, or it was no longer current. */
    final AtomicLong replayed = new AtomicLong();
    /** Executions closed while held: idle too long, too many, or replaced. */
    final AtomicLong dropped = new AtomicLong();

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
    Parked take(String codeType, String workflowId) {
        Map<String, Parked> held = byCodeType.get(codeType);
        return held == null ? null : held.remove(workflowId);
    }

    /**
     * Keeps an execution's lanes until its next continuation, for at most {@code idleMillis}. When
     * that makes more than {@code max} of its code type, the ones idle longest are closed.
     */
    void park(String codeType, String workflowId, Replayer replayer, String stamp, long idleMillis, int max) {
        Map<String, Parked> held = byCodeType.computeIfAbsent(codeType, k -> new ConcurrentHashMap<>());
        Parked replaced = held.put(workflowId,
                new Parked(replayer, stamp, clock.getAsLong() + idleMillis, parks.incrementAndGet()));
        if (replaced != null) {
            drop(replaced); // two workers threads were handed the same continuation
        }
        while (held.size() > Math.max(1, max)) {
            // a scan, only while too many are held
            Map.Entry<String, Parked> eldest = null;
            for (Map.Entry<String, Parked> e : held.entrySet()) {
                if (eldest == null || e.getValue().order() < eldest.getValue().order()) {
                    eldest = e;
                }
            }
            if (eldest == null) {
                break;
            }
            if (held.remove(eldest.getKey(), eldest.getValue())) {
                drop(eldest.getValue());
            }
        }
    }

    /** How many executions of a code type are held. */
    int held(String codeType) {
        Map<String, Parked> held = byCodeType.get(codeType);
        return held == null ? 0 : held.size();
    }

    /** Closes the executions that have sat idle past their time. */
    void dropIdle() {
        long now = clock.getAsLong();
        byCodeType.values().forEach(held -> held.forEach((workflowId, p) -> {
            if (p.until() <= now && held.remove(workflowId, p)) {
                drop(p);
            }
        }));
    }

    /** Closes every execution held: the process stops hosting. */
    void closeAll() {
        byCodeType.values().forEach(held -> held.forEach((workflowId, p) -> {
            if (held.remove(workflowId, p)) {
                p.replayer().close();
            }
        }));
    }

    /** Closing a replayer waits for its lanes' threads to unwind. */
    private void drop(Parked p) {
        dropped.incrementAndGet();
        p.replayer().close();
    }
}
