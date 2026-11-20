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

import java.util.List;

/** Inputs and outputs of the durability workload. */
public final class LedgerModel {

    private LedgerModel() {}

    /** One execution of the workload. {@code id} is also the idempotency key and correlation id. */
    public record LedgerInput(String id, int sleepSeconds) {}

    /**
     * What the workflow returns. {@code total} is the sum of its six steps, so a wrong or missing
     * step result shows up as a wrong total; {@code deposits} lists every signal it received, in
     * order; {@code bumps} counts the updates it applied.
     */
    public record LedgerResult(
            String id, String token, double random, long startedAt, int total, List<String> deposits, int bumps) {}

    /** The result every step returns: a pure function of the execution id and the step number. */
    public static int expectedStep(String id, int step) {
        return Math.floorMod(id.hashCode() * 31 + step * 7919, 100_000);
    }

    /** The total of a correct execution. */
    public static int expectedTotal(String id) {
        int total = 0;
        for (int step = 1; step <= LedgerWorkflow.STEPS; step++) {
            total += expectedStep(id, step);
        }
        return total;
    }
}
