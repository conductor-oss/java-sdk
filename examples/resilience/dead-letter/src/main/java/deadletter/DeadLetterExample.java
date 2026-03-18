package deadletter;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import deadletter.workers.HandleFailureWorker;
import deadletter.workers.ProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Dead Letter Queue Pattern — Capture and Retry Failed Tasks
 *
 * Demonstrates how to capture failed task information and route it to a
 * dead-letter handler workflow for logging, alerting, or retry logic.
 *
 * Two workflows:
 *   1. dead_letter_demo — main workflow with dl_process task
 *   2. dead_letter_handler — handler workflow with dl_handle_failure task
 *
 * Run:
 *   java -jar target/dead-letter-1.0.0.jar
 *   java -jar target/dead-letter-1.0.0.jar --workers
 */
public class DeadLetterExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Dead Letter Queue Pattern: Capture and Retry Failed Tasks ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef processTask = new TaskDef();
        processTask.setName("dl_process");
        processTask.setRetryCount(0);
        processTask.setTimeoutSeconds(60);
        processTask.setResponseTimeoutSeconds(30);
        processTask.setOwnerEmail("examples@orkes.io");

        TaskDef handleFailureTask = new TaskDef();
        handleFailureTask.setName("dl_handle_failure");
        handleFailureTask.setRetryCount(0);
        handleFailureTask.setTimeoutSeconds(60);
        handleFailureTask.setResponseTimeoutSeconds(30);
        handleFailureTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(processTask, handleFailureTask));

        System.out.println("\n  Registered: dl_process (no retries — failures go to dead letter)");
        System.out.println("  Registered: dl_handle_failure");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 — Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("workflow.json");
        client.registerWorkflow("dead-letter-handler.json");
        System.out.println("  Workflows registered: dead_letter_demo, dead_letter_handler\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ProcessWorker(), new HandleFailureWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run success scenario
        System.out.println("Step 4: Starting workflow (mode=success)...\n");
        String successId = client.startWorkflow("dead_letter_demo", 1,
                Map.of("mode", "success", "data", "order-123"));
        System.out.println("  Workflow ID: " + successId);

        Workflow successWf = client.waitForWorkflow(successId, "COMPLETED", 30000);
        System.out.println("  Status: " + successWf.getStatus().name());
        System.out.println("  Output: " + successWf.getOutput() + "\n");

        // Step 5 — Run failure scenario (triggers dead letter handler)
        System.out.println("Step 5: Starting workflow (mode=fail)...\n");
        String failId = client.startWorkflow("dead_letter_demo", 1,
                Map.of("mode", "fail", "data", "order-456"));
        System.out.println("  Workflow ID: " + failId);

        Workflow failWf = client.waitForWorkflow(failId, "FAILED", 30000);
        System.out.println("  Status: " + failWf.getStatus().name());
        System.out.println("  Output: " + failWf.getOutput() + "\n");

        // Step 6 — Start the dead letter handler for the failed workflow
        System.out.println("Step 6: Starting dead letter handler workflow...\n");
        String handlerId = client.startWorkflow("dead_letter_handler", 1,
                Map.of("failedWorkflowId", failId,
                        "failedTaskName", "dl_process",
                        "error", "Processing failed for data: order-456"));
        System.out.println("  Handler Workflow ID: " + handlerId);

        Workflow handlerWf = client.waitForWorkflow(handlerId, "COMPLETED", 30000);
        System.out.println("  Status: " + handlerWf.getStatus().name());
        System.out.println("  Output: " + handlerWf.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(successWf.getStatus().name())
                && "COMPLETED".equals(handlerWf.getStatus().name())) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
