package errorclassification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import errorclassification.workers.ApiCallWorker;
import errorclassification.workers.ErrorHandlerWorker;

import java.util.List;
import java.util.Map;

/**
 * Error Classification — Retryable vs Non-Retryable Failures
 *
 * Demonstrates how to classify API errors as retryable (429, 503) or
 * non-retryable (400) and route them through different workflow paths.
 *
 * - Retryable errors: task FAILS and Conductor retries automatically
 * - Non-retryable errors: task COMPLETES with errorType="non_retryable",
 *   and the workflow routes to an error handler via a SWITCH task
 *
 * Run:
 *   java -jar target/error-classification-1.0.0.jar
 *   java -jar target/error-classification-1.0.0.jar --workers
 */
public class ErrorClassificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Error Classification Demo: Retryable vs Non-Retryable Failures ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef apiCallTask = new TaskDef();
        apiCallTask.setName("ec_api_call");
        apiCallTask.setRetryCount(3);
        apiCallTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        apiCallTask.setRetryDelaySeconds(1);
        apiCallTask.setTimeoutSeconds(60);
        apiCallTask.setResponseTimeoutSeconds(30);
        apiCallTask.setOwnerEmail("examples@orkes.io");

        TaskDef handleErrorTask = new TaskDef();
        handleErrorTask.setName("ec_handle_error");
        handleErrorTask.setRetryCount(0);
        handleErrorTask.setTimeoutSeconds(60);
        handleErrorTask.setResponseTimeoutSeconds(30);
        handleErrorTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(apiCallTask, handleErrorTask));

        System.out.println("\n  Registered: ec_api_call (retries: 3, FIXED 1s delay)");
        System.out.println("  Registered: ec_handle_error (no retries)\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'error_classification_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ApiCallWorker(), new ErrorHandlerWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Test: successful API call (no error)
        System.out.println("Step 4: Starting workflow with no error...\n");
        String wfId1 = client.startWorkflow("error_classification_demo", 1,
                Map.of("triggerError", ""));
        System.out.println("  Workflow ID: " + wfId1);
        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Output: " + wf1.getOutput() + "\n");

        // Step 5 — Test: non-retryable security-posture error (routes to error handler)
        System.out.println("Step 5: Starting workflow with triggerError=security-posture...\n");
        String wfId2 = client.startWorkflow("error_classification_demo", 1,
                Map.of("triggerError", "security-posture"));
        System.out.println("  Workflow ID: " + wfId2);
        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        System.out.println("  Status: " + wf2.getStatus().name());
        System.out.println("  Output: " + wf2.getOutput() + "\n");

        client.stopWorkers();

        boolean allPassed = "COMPLETED".equals(wf1.getStatus().name())
                && "COMPLETED".equals(wf2.getStatus().name());

        if (allPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
