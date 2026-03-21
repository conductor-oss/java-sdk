package contentreviewpipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import contentreviewpipeline.workers.CrpAiDraftWorker;
import contentreviewpipeline.workers.CrpPublishWorker;

import java.util.List;
import java.util.Map;

/**
 * Content Review Pipeline -- AI Draft, Human Edit, Publish
 *
 * Demonstrates a workflow that:
 *   1. AI generates a content draft (crp_ai_draft)
 *   2. Waits for human review/edit (WAIT task: crp_human_review)
 *   3. Publishes the content if approved (crp_publish)
 *
 * Run:
 *   java -jar target/content-review-pipeline-1.0.0.jar
 *   java -jar target/content-review-pipeline-1.0.0.jar --workers
 */
public class ContentReviewPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Content Review Pipeline: AI Draft -> Human Edit -> Publish ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef aiDraftTask = new TaskDef();
        aiDraftTask.setName("crp_ai_draft");
        aiDraftTask.setTimeoutSeconds(60);
        aiDraftTask.setResponseTimeoutSeconds(30);
        aiDraftTask.setOwnerEmail("examples@orkes.io");

        TaskDef publishTask = new TaskDef();
        publishTask.setName("crp_publish");
        publishTask.setTimeoutSeconds(60);
        publishTask.setResponseTimeoutSeconds(30);
        publishTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(aiDraftTask, publishTask));

        System.out.println("  Registered: crp_ai_draft, crp_publish\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'content_review_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new CrpAiDraftWorker(), new CrpPublishWorker());
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
        String workflowId = client.startWorkflow("content_review_pipeline", 1,
                Map.of("topic", "AI in Healthcare", "audience", "developers"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion (WAIT task requires external signal)...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
