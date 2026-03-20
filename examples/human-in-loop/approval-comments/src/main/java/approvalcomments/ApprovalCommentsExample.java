package approvalcomments;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import approvalcomments.workers.PrepareDocWorker;
import approvalcomments.workers.ApplyFeedbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Approval with Comments and Attachments -- Human-in-the-Loop demo
 *
 * Demonstrates a workflow where a document is prepared, then a human
 * reviewer provides a decision with comments, attachments, ratings,
 * and tags via a WAIT task.
 *
 * Flow: prepare_doc -> WAIT (review_with_comments) -> apply_feedback
 *
 * Run:
 *   java -jar target/approval-comments-1.0.0.jar
 *   java -jar target/approval-comments-1.0.0.jar --workers
 */
public class ApprovalCommentsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Approval with Comments and Attachments Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareDocTask = new TaskDef();
        prepareDocTask.setName("ac_prepare_doc");
        prepareDocTask.setRetryCount(0);
        prepareDocTask.setTimeoutSeconds(60);
        prepareDocTask.setResponseTimeoutSeconds(30);
        prepareDocTask.setOwnerEmail("examples@orkes.io");

        TaskDef applyFeedbackTask = new TaskDef();
        applyFeedbackTask.setName("ac_apply_feedback");
        applyFeedbackTask.setRetryCount(0);
        applyFeedbackTask.setTimeoutSeconds(60);
        applyFeedbackTask.setResponseTimeoutSeconds(30);
        applyFeedbackTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareDocTask, applyFeedbackTask));
        System.out.println("  Registered: ac_prepare_doc, ac_apply_feedback\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'approval_comments_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PrepareDocWorker(), new ApplyFeedbackWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("approval_comments_demo", 1,
                Map.of("documentId", "DOC-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion (will pause at WAIT task)
        System.out.println("Step 5: Waiting for workflow (will pause at WAIT task for human input)...");
        System.out.println("  Use the Conductor UI to complete the WAIT task with review data.\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: WORKFLOW PAUSED (waiting for human input)");
            System.exit(0);
        }
    }
}
