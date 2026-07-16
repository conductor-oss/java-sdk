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
package org.conductoross.conductor.ai.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WorkerManager#effectiveTaskTimeout(int)} — the rule that
 * sizes a Conductor task def's timeout/responseTimeout to a handler's configured
 * blocking timeout so the server's patience can never drift below the worker's.
 */
class WorkerManagerTimeoutTest {

    @Test
    void unconfigured_uses_safe_default() {
        assertEquals(300, WorkerManager.effectiveTaskTimeout(0), "0 (unset) must fall back to the 300s default");
        assertEquals(300, WorkerManager.effectiveTaskTimeout(-5), "negative must fall back to the 300s default");
    }

    @Test
    void short_timeouts_keep_the_300s_floor() {
        assertEquals(300, WorkerManager.effectiveTaskTimeout(30));
        assertEquals(300, WorkerManager.effectiveTaskTimeout(240)); // 240 + 60 == 300
    }

    @Test
    void long_timeouts_raise_the_ceiling_above_300_with_slack() {
        assertEquals(301, WorkerManager.effectiveTaskTimeout(241)); // 241 + 60
        assertEquals(360, WorkerManager.effectiveTaskTimeout(300)); // 300 + 60
        assertEquals(660, WorkerManager.effectiveTaskTimeout(600)); // 600 + 60
    }

    @Test
    void server_patience_always_exceeds_the_handler_timeout() {
        for (int t : new int[] {1, 100, 300, 1000, 5000}) {
            assertTrue(
                    WorkerManager.effectiveTaskTimeout(t) >= t + WorkerManager.TASK_TIMEOUT_SLACK_SECONDS,
                    "effective timeout for " + t + "s must leave at least the slack margin");
        }
    }
}
