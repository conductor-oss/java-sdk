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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.executor.task.TaskContext;
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
 * workers. The call resolves to the @WorkerTask name, the arguments ship as task input keyed by
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
        int checks = checked(runId);
        boolean done = checks >= 3;
        return new JobStatus(done, done ? "finished after " + checks + " checks" : "still running");
    }

    private final Map<String, Integer> checks = new ConcurrentHashMap<>();

    /**
     * How many times a run's job was checked: counted in this process, or, with {@code
     * KITCHENSINK_STATE_DIR} (the failover bench, which kills and restarts the workers), in a file
     * there, as a real job's status lives outside the worker that asks for it.
     */
    private int checked(String runId) {
        String dir = System.getenv("KITCHENSINK_STATE_DIR");
        if (dir == null) {
            return checks.merge(runId, 1, Integer::sum);
        }
        Path file = Path.of(dir, "checks-" + runId.replaceAll("[^A-Za-z0-9_.-]", "_"));
        try {
            String before = Files.exists(file) ? Files.readString(file).trim() : "";
            int n = (before.isEmpty() ? 0 : Integer.parseInt(before)) + 1;
            Path next = Files.createTempFile(Path.of(dir), "checks", ".tmp");
            Files.writeString(next, Integer.toString(n));
            Files.move(next, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return n;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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

    private static final long SLOW_MS = Long.parseLong(System.getenv().getOrDefault("KITCHENSINK_SLOW_MS", "3000"));

    /**
     * Long enough that a join that does not wait for this task is caught finishing first: 3 s, or
     * {@code KITCHENSINK_SLOW_MS}. The failover bench makes it a long-running task and kills the
     * worker running it; another worker runs it again once its response timeout passes.
     */
    @WorkerTask(value = "slow_step", threadCount = 4, responseTimeoutSeconds = 60)
    public String slowStep(String label) {
        TaskContext context = TaskContext.get();
        if (context != null && System.getenv("CONDUCTOR_WORKER_ID") != null) {
            // tasks report the host name as their worker id, so the bench finds the process here
            System.out.println("slow_step " + context.getTaskId() + " running on " + System.getenv("CONDUCTOR_WORKER_ID"));
        }
        try {
            Thread.sleep(SLOW_MS);
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
