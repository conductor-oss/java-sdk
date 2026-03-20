package contentpublishing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentpublishing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Content Publishing Workflow Demo
 *
 * Publishing pipeline: draft content, review, approve, format for
 * distribution, publish, and distribute to channels.
 *
 * Run:
 *   java -jar target/content-publishing-1.0.0.jar
 */
public class ContentPublishingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 511: Content Publishing ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pub_draft_content", "pub_review_content", "pub_approve_content",
                "pub_format_content", "pub_publish_content", "pub_distribute_content"));
        System.out.println("  Registered 6 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'content_publishing_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DraftContentWorker(),
                new ReviewContentWorker(),
                new ApproveContentWorker(),
                new FormatContentWorker(),
                new PublishContentWorker(),
                new DistributeContentWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("content_publishing_workflow", 1,
                Map.of("contentId", "CNT-511-001",
                        "authorId", "AUTH-220",
                        "contentType", "blog_post",
                        "title", "Optimizing Cloud Costs in 2026"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
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
