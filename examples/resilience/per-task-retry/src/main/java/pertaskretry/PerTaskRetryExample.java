package pertaskretry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import pertaskretry.workers.*;

import java.util.*;

/**
 * Per-Task Retry Configuration — Different retry configs per task in same workflow.
 *
 * Demonstrates how Conductor lets you configure different retry strategies for each task
 * in a workflow. Each task gets its own retryCount, retryLogic (FIXED or EXPONENTIAL_BACKOFF),
 * and retryDelaySeconds — all set in task-defs.json, not in code.
 *
 * Tasks:
 *   ptr_validate — retryCount:1, FIXED, 1s delay
 *   ptr_payment  — retryCount:5, EXPONENTIAL_BACKOFF, 1s base delay
 *   ptr_notify   — retryCount:3, FIXED, 2s delay
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/per-task-retry-1.0.0.jar
 */
public class PerTaskRetryExample {

    private static final List<String> TASK_NAMES = List.of(
            "ptr_validate",
            "ptr_payment",
            "ptr_notify"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new PtrValidate(),
                new PtrPayment(),
                new PtrNotify()
        );
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Per-Task Retry Demo: Different Retry Configs Per Task ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions (with per-task retry configs)
        System.out.println("Step 1: Registering task definitions with per-task retry configs...");
        client.registerTaskDefsFromResource("task-defs.json");
        System.out.println("  ptr_validate : retryCount=1, FIXED, 1s delay");
        System.out.println("  ptr_payment  : retryCount=5, EXPONENTIAL_BACKOFF, 1s base delay");
        System.out.println("  ptr_notify   : retryCount=3, FIXED, 2s delay\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'per_task_retry_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join(); // Block forever
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting per_task_retry_demo workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("orderId", "ORD-20260314-001");

        String workflowId = client.startWorkflow("per_task_retry_demo", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Results ---");
            Map<String, Object> validation = (Map<String, Object>) output.get("validation");
            Map<String, Object> payment = (Map<String, Object>) output.get("payment");
            Map<String, Object> notification = (Map<String, Object>) output.get("notification");

            if (validation != null) {
                System.out.println("  Validation   : " + validation.get("result")
                        + " (orderId: " + validation.get("orderId") + ")");
            }
            if (payment != null) {
                System.out.println("  Payment      : " + payment.get("result")
                        + " (attempt: " + payment.get("attempt") + ")");
            }
            if (notification != null) {
                System.out.println("  Notification : " + notification.get("result")
                        + " (orderId: " + notification.get("orderId") + ")");
            }
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nWorkflow did not complete (status: " + status + ")");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.out.println("Result: WORKFLOW_ERROR");
            System.exit(1);
        }

        System.out.println("\nResult: SUCCESS — all tasks completed with their per-task retry configs");
        System.exit(0);
    }
}
