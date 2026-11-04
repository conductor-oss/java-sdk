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
package com.netflix.conductor.client.automator;

/**
 * Capacity granted to one poll request by {@link AdaptiveConcurrencyController#reserve}. Each slot is owned by the
 * request until it is released by {@link AdaptiveConcurrencyController#completePoll} or transferred to a returned
 * task.
 *
 * @param queue the logical queue the slots belong to
 * @param count number of tasks the poll may request; every one has an open slot
 * @param kind  what the poll is for, which sets how long the server may hold it
 */
record SlotReservation(WorkerQueue queue, int count, Kind kind) {

    enum Kind {
        /** Slots within the queue's share; the poll waits the worker's poll timeout. */
        SHARE,
        /** A queue without free share keeps its domain active: one task, without waiting. */
        HEARTBEAT,
        /** A queue without free share waits for its next task on a slot no share claims: one task, standby timeout. */
        STANDBY
    }

    boolean heartbeat() {
        return kind == Kind.HEARTBEAT;
    }

    boolean standby() {
        return kind == Kind.STANDBY;
    }
}
