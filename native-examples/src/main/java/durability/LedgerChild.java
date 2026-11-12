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

import org.conductoross.conductor.sdk.Workflow;


/** A child workflow: runs step 5 of its parent's execution. */
public class LedgerChild {

    // Conductor passes a stand-in whose calls schedule the tasks; the real LedgerTasks runs on the workers.
    private final LedgerTasks tasks;

    public LedgerChild(LedgerTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "durability_ledger_child", version = 1, ownerEmail = "bench@example.com", timeout = "PT2H")
    public int run(String id) {
        return tasks.step(id, 5);
    }
}
