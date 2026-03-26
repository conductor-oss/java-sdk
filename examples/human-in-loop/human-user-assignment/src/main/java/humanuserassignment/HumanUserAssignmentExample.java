package humanuserassignment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import humanuserassignment.workers.HuaPrepareWorker;
import humanuserassignment.workers.HuaPostReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * HUMAN Task with User Assignment — assigns a WAIT task to a specific user
 *
 * Demonstrates a workflow where a document is prepared, then a WAIT task
 * is assigned to a specific user for review, and finally the result is
 * finalized after the human completes the review.
 *
 * Workflow: prepare -> WAIT (assigned review with assignedTo) -> post_review
 *
 * Run:
 *   java -jar target/human-user-assignment-1.0.0.jar
 *   java -jar target/human-user-assignment-1.0.0.jar --workers
 */
public class HumanUserAssignmentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== HUMAN Task with User Assignment Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareTask = new TaskDef();
        prepareTask.setName("hua_prepare");
        prepareTask.setTimeoutSeconds(60);
        prepareTask.setResponseTimeoutSeconds(30);
        prepareTask.setOwnerEmail("examples@orkes.io");

        TaskDef postReviewTask = new TaskDef();
        postReviewTask.setName("hua_post_review");
        postReviewTask.setTimeoutSeconds(60);
        postReviewTask.setResponseTimeoutSeconds(30);
        postReviewTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareTask, postReviewTask));

        System.out.println("  Registered: hua_prepare, hua_post_review\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'human_user_assignment_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new HuaPrepareWorker(), new HuaPostReviewWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("human_user_assignment_demo", 1,
                Map.of("documentId", "DOC-12345", "assignedTo", "reviewer@example.com"));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  The workflow will pause at the WAIT task until a human completes the review.\n");

        // Step 5 — Wait for WAIT task (will stay in RUNNING until manually completed)
        System.out.println("Step 5: Workflow is now paused at the WAIT task.");
        System.out.println("  The WAIT task is assigned to: reviewer@example.com");
        System.out.println("  Complete it via the Conductor UI or API to continue.\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 10000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: Workflow is waiting for human input (expected).");
            System.exit(0);
        }
    }
}
