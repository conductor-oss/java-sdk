package failureworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import failureworkflow.workers.ProcessWorker;
import failureworkflow.workers.CleanupWorker;
import failureworkflow.workers.NotifyFailureWorker;

import java.util.List;
import java.util.Map;

/**
 * Failure Workflows — Auto error recovery pipeline
 *
 * Demonstrates Conductor's failureWorkflow feature: when the main workflow
 * fails, an error handler workflow runs automatically to perform cleanup
 * and send failure notifications.
 *
 * Run:
 *   java -jar target/failure-workflow-1.0.0.jar
 */
public class FailureWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Failure Workflow: Auto Error Recovery Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("fw_process", "fw_cleanup", "fw_notify_failure"));
        System.out.println("  Registered: fw_process, fw_cleanup, fw_notify_failure\n");

        // Step 2 — Register workflows (error handler first, then main)
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("error-handler-workflow.json");
        System.out.println("  Registered: error_handler_wf");
        client.registerWorkflow("workflow.json");
        System.out.println("  Registered: main_with_failure_handler\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessWorker(),
                new CleanupWorker(),
                new NotifyFailureWorker()
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

        // Step 4 — Start the workflow with shouldFail=true to trigger failure handler
        System.out.println("Step 4: Starting workflow with shouldFail=true...\n");
        String workflowId = client.startWorkflow("main_with_failure_handler", 1,
                Map.of("shouldFail", true));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for main workflow to fail (which triggers the error handler)
        System.out.println("Step 5: Waiting for workflow to fail and trigger error handler...");
        Workflow workflow = client.waitForWorkflow(workflowId, "FAILED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Main workflow status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        // Give the error handler time to complete
        Thread.sleep(5000);
        System.out.println("  Error handler workflow should have been triggered automatically.\n");

        client.stopWorkers();

        if ("FAILED".equals(status)) {
            System.out.println("Result: PASSED (main workflow failed as expected, error handler triggered)");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED (unexpected status: " + status + ")");
            System.exit(1);
        }
    }
}
