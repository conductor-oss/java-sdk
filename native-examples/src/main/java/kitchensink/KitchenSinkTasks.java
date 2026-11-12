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
package kitchensink;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import kitchensink.KitchenSinkModel.DynamicTask;
import kitchensink.KitchenSinkModel.FanoutPlan;
import kitchensink.KitchenSinkModel.JobStatus;
import kitchensink.KitchenSinkModel.OddEven;
import kitchensink.KitchenSinkModel.Receipt;
import kitchensink.KitchenSinkModel.Stats;
import kitchensink.KitchenSinkModel.Summary;

/**
 * The tasks. This is today's worker style, unchanged: a plain class with @WorkerTask methods,
 * one poller per method, ordinary code inside (HTTP clients, database calls, anything blocking).
 *
 * A workflow takes this class as a constructor parameter and calls the tasks directly:
 *
 *     Stats stats = tasks.task1(mod, oddEven);
 *
 * What it gets is a stand-in whose calls schedule the tasks; this class itself runs on the
 * workers. task(KitchenSinkTasks::task1, mod, oddEven) does the same by method reference. The
 * call resolves to the @WorkerTask name, the arguments ship as task input keyed by
 * parameter name (compile with -parameters, or use @InputParam as today), and the return value
 * comes back as task output. Argument and return types come from the method itself through
 * reflection, so nothing depends on generics surviving compilation. Any other worker, in any
 * language, can serve the same task names.
 *
 * retryCount, retryDelay and responseTimeout on @WorkerTask are proposed additions: they become
 * the task definition on registration, so there is no separate TaskDef to maintain.
 */
public class KitchenSinkTasks {

    @WorkerTask("task_1")
    public Stats task1(int mod, String oddEven) {
        return new Stats(mod, oddEven);
    }

    @WorkerTask("task_4")
    public FanoutPlan task4(OddEven oddEven) {
        return new FanoutPlan(List.of(
                new DynamicTask("task_5", Map.of("mod", oddEven.mod(), "oddEven", oddEven.oddEven())),
                new DynamicTask("task_6", Map.of("previous", "from task_4"))));
    }

    /** Serves both the dynamic task_2 call, which expects OddEven, and the sub flow. */
    @WorkerTask("task_5")
    public OddEven task5(int mod, String oddEven) {
        return new OddEven(mod, oddEven);
    }

    @WorkerTask("task_6")
    public String task6(String previous) {
        return "task_6 after " + previous;
    }

    @WorkerTask("task_10")
    public void task10() {
        System.out.println("task_10");
    }

    @WorkerTask("task_11")
    public void task11() {
        System.out.println("task_11");
    }

    @WorkerTask("task_30")
    public Summary task30(List<String> statuses, List<String> workflowIds) {
        return new Summary(statuses.size() + workflowIds.size());
    }

    /** Polled by a server-side loop; see KitchenSinkWorkflow. */
    @WorkerTask("check_status")
    public JobStatus checkStatus(String runId) {
        // the job finishes on the third check of each run
        int checks = this.checks.merge(runId, 1, Integer::sum);
        boolean done = checks >= 3;
        return new JobStatus(done, done ? "finished after " + checks + " checks" : "still running");
    }

    private final java.util.Map<String, Integer> checks = new java.util.concurrent.ConcurrentHashMap<>();

    /** No retries: a failed charge must surface to the workflow, which compensates. */
    @WorkerTask(value = "charge", retryCount = 0)
    public Receipt charge(String runId, int amount) {
        if (amount > 1_000) {
            // FAILED_WITH_TERMINAL_ERROR: no retry, the workflow sees TaskFailure at once
            throw new NonRetryableException("amount over limit: " + amount);
        }
        return new Receipt(UUID.randomUUID().toString(), amount);
    }

    /** Called once per item by a loop; see section 3 of KitchenSinkWorkflow. */
    @WorkerTask("label_item")
    public String labelItem(String item) {
        return "LABEL[" + item + "]";
    }

    /** Returns its label: the composition section of the workflow is about shape, not work. */
    @WorkerTask("step")
    public String step(String label) {
        return label;
    }

    /** Long enough that a join that does not wait for this task is caught finishing first. */
    @WorkerTask("slow_step")
    public String slowStep(String label) {
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return label;
    }

    @WorkerTask("refund")
    public void refund(String runId, int amount) {
        System.out.println("refunded " + amount + " for " + runId);
    }
}
