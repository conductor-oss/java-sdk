package responsetimeout;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import responsetimeout.workers.RespTimeoutWorker;

import java.util.List;
import java.util.Map;

/**
 * Response Timeout — Detect Stuck Workers
 *
 * responseTimeoutSeconds defines how long Conductor waits for a worker
 * to respond after picking up a task. If the worker goes silent
 * (crashes, hangs, network issue), the task is marked timed out
 * and retried.
 *
 * Run:
 *   java -jar target/response-timeout-1.0.0.jar
 *   java -jar target/response-timeout-1.0.0.jar --workers
 */
public class ResponseTimeoutExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Response Timeout: Detect Stuck Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with responseTimeoutSeconds
        System.out.println("Step 1: Registering task definition...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("resp_timeout_task");
        taskDef.setRetryCount(2);
        taskDef.setRetryLogic(TaskDef.RetryLogic.FIXED);
        taskDef.setRetryDelaySeconds(1);
        taskDef.setTimeoutSeconds(30);
        taskDef.setResponseTimeoutSeconds(3);  // Worker must respond within 3 seconds
        taskDef.setOwnerEmail("examples@orkes.io");
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  responseTimeoutSeconds: " + taskDef.getResponseTimeoutSeconds());
        System.out.println("  If a worker takes longer than 3s to respond, the task times out.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'resp_timeout_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting worker...");
        RespTimeoutWorker worker = new RespTimeoutWorker();
        List<Worker> workers = List.of(worker);
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Worker responds quickly (within timeout)
        System.out.println("Step 4: Starting workflow — worker responds quickly (within timeout)...\n");
        String workflowId = client.startWorkflow("resp_timeout_demo", 1,
                Map.of("mode", "fast"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n--- How response timeout works ---");
        System.out.println("  1. Task is polled by worker (IN_PROGRESS)");
        System.out.println("  2. Conductor starts a " + taskDef.getResponseTimeoutSeconds() + "s timer");
        System.out.println("  3a. Worker responds in time -> task completes normally");
        System.out.println("  3b. Worker goes silent -> task times out -> retried (if retries remain)");
        System.out.println("\n  Use cases:");
        System.out.println("  - Detect crashed workers");
        System.out.println("  - Detect network partitions");
        System.out.println("  - Detect hung processes");
        System.out.println("  - Set responseTimeoutSeconds < timeoutSeconds ("
                + taskDef.getResponseTimeoutSeconds() + "s < " + taskDef.getTimeoutSeconds() + "s)");

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
