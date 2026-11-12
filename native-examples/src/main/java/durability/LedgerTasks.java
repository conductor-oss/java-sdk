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
package durability;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * The workload's one task. It sleeps a random time, fails transiently at a configured rate, and
 * returns {@link LedgerModel#expectedStep}. Every attempt is logged to the {@link Ledger}, which is
 * how the checker knows what actually ran, independently of what the server says.
 *
 * <p>{@code BENCH_STEP_MAX_MS} (default 800) bounds the sleep; {@code BENCH_STEP_FAIL_RATE}
 * (default 0.1) is the chance an attempt throws.
 */
public class LedgerTasks {

    private static final int MAX_MS = Integer.parseInt(System.getenv().getOrDefault("BENCH_STEP_MAX_MS", "800"));
    private static final double FAIL_RATE =
            Double.parseDouble(System.getenv().getOrDefault("BENCH_STEP_FAIL_RATE", "0.1"));

    @WorkerTask(value = "durability_step", retryCount = 8, retryDelaySeconds = 1, responseTimeoutSeconds = 20)
    public int step(String id, int step) {
        String key = id + ":" + step;
        String nonce = UUID.randomUUID().toString();
        Ledger.taskStart(key, nonce);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (MAX_MS > 0) {
            try {
                Thread.sleep(random.nextInt(MAX_MS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
        }
        if (random.nextDouble() < FAIL_RATE) {
            Ledger.taskFail(key, nonce, "injected transient failure");
            throw new IllegalStateException("injected transient failure in " + key);
        }
        int value = LedgerModel.expectedStep(id, step);
        Ledger.taskEnd(key, nonce, value);
        return value;
    }
}
