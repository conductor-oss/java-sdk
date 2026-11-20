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
import java.util.Random;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import kitchensink.KitchenSinkModel.FanoutPlan;
import kitchensink.KitchenSinkModel.JobStatus;
import kitchensink.KitchenSinkModel.OddEven;
import kitchensink.KitchenSinkModel.Receipt;
import kitchensink.KitchenSinkModel.Stats;
import kitchensink.KitchenSinkModel.Summary;

/**
 * The kitchensink's tasks with failures injected, for the chaos run: each call first draws, and
 * may throw an error the server retries ({@code CHAOS_TASK_ERRORS}, a probability per call) or a
 * terminal failure it does not ({@code CHAOS_TASK_FAILURES}). A terminal failure reaches the
 * workflow code as a TaskFailure: caught where the code catches it, otherwise it fails the run at
 * that task. Draws are seeded by {@code CHAOS_SEED} and the worker id.
 */
public class ChaosTasks extends KitchenSinkTasks {

    private final double errors = Double.parseDouble(System.getenv().getOrDefault("CHAOS_TASK_ERRORS", "0"));
    private final double failures = Double.parseDouble(System.getenv().getOrDefault("CHAOS_TASK_FAILURES", "0"));
    private final Random random = new Random(
            (System.getenv().getOrDefault("CHAOS_SEED", "0") + "/" + System.getenv().getOrDefault("CONDUCTOR_WORKER_ID", "")).hashCode());

    static boolean enabled() {
        return System.getenv("CHAOS_TASK_ERRORS") != null || System.getenv("CHAOS_TASK_FAILURES") != null;
    }

    private void draw(String task) {
        double d;
        synchronized (random) {
            d = random.nextDouble();
        }
        if (d < failures) {
            throw new NonRetryableException("injected terminal failure in " + task);
        }
        if (d < failures + errors) {
            throw new IllegalStateException("injected error in " + task);
        }
    }

    @Override @WorkerTask("task_1")
    public Stats task1(int mod, String oddEven) { draw("task_1"); return super.task1(mod, oddEven); }

    @Override @WorkerTask("task_4")
    public FanoutPlan task4(OddEven oddEven) { draw("task_4"); return super.task4(oddEven); }

    @Override @WorkerTask("task_5")
    public OddEven task5(int mod, String oddEven) { draw("task_5"); return super.task5(mod, oddEven); }

    @Override @WorkerTask("task_6")
    public String task6(String previous) { draw("task_6"); return super.task6(previous); }

    @Override @WorkerTask("task_10")
    public void task10() { draw("task_10"); super.task10(); }

    @Override @WorkerTask("task_11")
    public void task11() { draw("task_11"); super.task11(); }

    @Override @WorkerTask("task_30")
    public Summary task30(List<String> statuses, List<String> workflowIds) { draw("task_30"); return super.task30(statuses, workflowIds); }

    @Override @WorkerTask("check_status")
    public JobStatus checkStatus(String runId) { draw("check_status"); return super.checkStatus(runId); }

    @Override @WorkerTask(value = "charge", retryCount = 0)
    public Receipt charge(String runId, int amount) { draw("charge"); return super.charge(runId, amount); }

    @Override @WorkerTask("label_item")
    public String labelItem(String item) { draw("label_item"); return super.labelItem(item); }

    @Override @WorkerTask("step")
    public String step(String label) { draw("step"); return super.step(label); }

    @Override @WorkerTask("slow_step")
    public String slowStep(String label) { draw("slow_step"); return super.slowStep(label); }

    @Override @WorkerTask("refund")
    public void refund(String runId, int amount) { draw("refund"); super.refund(runId, amount); }
}
