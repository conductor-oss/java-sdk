package timeoutpolicies;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import timeoutpolicies.workers.CriticalWorker;
import timeoutpolicies.workers.OptionalWorker;
import timeoutpolicies.workers.RetryableWorker;

import java.util.List;
import java.util.Map;

/**
 * Timeout Policies — TIME_OUT_WF vs ALERT_ONLY vs RETRY
 *
 * When a task times out, what happens next depends on timeoutPolicy:
 * - TIME_OUT_WF: Entire workflow is terminated (default)
 * - ALERT_ONLY: Task is marked failed but workflow continues
 * - RETRY: Task is retried automatically
 *
 * Registers 3 task defs with different timeout policies and runs
 * a workflow using tp_critical to verify normal operation.
 *
 * Run:
 *   java -jar target/timeout-policies-1.0.0.jar
 *   java -jar target/timeout-policies-1.0.0.jar --workers
 */
public class TimeoutPoliciesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Timeout Policies Demo: TIME_OUT_WF vs ALERT_ONLY vs RETRY ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register 3 task definitions with different timeout policies
        System.out.println("Step 1: Registering task definitions with timeout policies...");

        // tp_critical — TIME_OUT_WF (default: workflow terminates on timeout)
        TaskDef criticalTask = new TaskDef();
        criticalTask.setName("tp_critical");
        criticalTask.setRetryCount(0);
        criticalTask.setTimeoutSeconds(30);
        criticalTask.setResponseTimeoutSeconds(10);
        criticalTask.setTimeoutPolicy(TaskDef.TimeoutPolicy.TIME_OUT_WF);
        criticalTask.setOwnerEmail("examples@orkes.io");

        // tp_optional — ALERT_ONLY (task fails, workflow continues)
        TaskDef optionalTask = new TaskDef();
        optionalTask.setName("tp_optional");
        optionalTask.setRetryCount(0);
        optionalTask.setTimeoutSeconds(30);
        optionalTask.setResponseTimeoutSeconds(10);
        optionalTask.setTimeoutPolicy(TaskDef.TimeoutPolicy.ALERT_ONLY);
        optionalTask.setOwnerEmail("examples@orkes.io");

        // tp_retryable — RETRY (auto-retry on timeout)
        TaskDef retryableTask = new TaskDef();
        retryableTask.setName("tp_retryable");
        retryableTask.setRetryCount(2);
        retryableTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        retryableTask.setRetryDelaySeconds(1);
        retryableTask.setTimeoutSeconds(30);
        retryableTask.setResponseTimeoutSeconds(10);
        retryableTask.setTimeoutPolicy(TaskDef.TimeoutPolicy.RETRY);
        retryableTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(criticalTask, optionalTask, retryableTask));

        System.out.println("\n  Registered 3 task definitions:\n");
        System.out.println("  tp_critical:");
        System.out.println("    timeoutPolicy: TIME_OUT_WF (default)");
        System.out.println("    Task timeout -> workflow TIMED_OUT");
        System.out.println("    Use for: critical tasks where failure = workflow failure");
        System.out.println("");
        System.out.println("  tp_optional:");
        System.out.println("    timeoutPolicy: ALERT_ONLY");
        System.out.println("    Task timeout -> task marked TIMED_OUT, workflow continues");
        System.out.println("    Use for: optional enrichment, logging, non-critical steps");
        System.out.println("");
        System.out.println("  tp_retryable:");
        System.out.println("    timeoutPolicy: RETRY");
        System.out.println("    Task timeout -> automatic retry (up to retryCount)");
        System.out.println("    Use for: flaky services, intermittent network issues");
        System.out.println("");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'timeout_policy_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CriticalWorker(),
                new OptionalWorker(),
                new RetryableWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow with TIME_OUT_WF policy
        System.out.println("Step 4: Starting workflow with TIME_OUT_WF policy...\n");
        String workflowId = client.startWorkflow(
                "timeout_policy_demo", 1, Map.of("mode", "critical"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n--- Timeout Policy Reference ---");
        System.out.println("  Recommended combinations:");
        System.out.println("    Payment task:  TIME_OUT_WF + retryCount:3 + EXPONENTIAL_BACKOFF");
        System.out.println("    Logging task:  ALERT_ONLY  + retryCount:0");
        System.out.println("    API call:      RETRY       + retryCount:5 + FIXED delay");

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
