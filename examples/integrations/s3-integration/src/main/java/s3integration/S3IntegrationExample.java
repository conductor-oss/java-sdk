package s3integration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import s3integration.workers.S3UploadWorker;
import s3integration.workers.SetMetadataWorker;
import s3integration.workers.GenerateUrlWorker;
import s3integration.workers.S3NotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 447: S3 Integration
 *
 * Runs an S3 storage integration workflow:
 * upload object -> set metadata -> generate presigned URL -> notify.
 *
 * Run:
 *   java -jar target/s3-integration-1.0.0.jar
 */
public class S3IntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 447: S3 Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "s3_upload", "s3_set_metadata", "s3_generate_url", "s3_notify"));
        System.out.println("  Registered: s3_upload, s3_set_metadata, s3_generate_url, s3_notify\n");

        System.out.println("Step 2: Registering workflow \'s3_integration_447\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new S3UploadWorker(),
                new SetMetadataWorker(),
                new GenerateUrlWorker(),
                new S3NotifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("s3_integration_447", 1,
                Map.of("bucket", "company-reports",
                        "key", "quarterly/Q4-2024-report.pdf",
                        "contentType", "application/pdf",
                        "notifyEmail", "manager@example.com"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  ETag: " + workflow.getOutput().get("etag"));
        System.out.println("  Presigned URL: " + String.valueOf(workflow.getOutput().get("presignedUrl")).substring(0, Math.min(60, String.valueOf(workflow.getOutput().get("presignedUrl")).length())) + "...");
        System.out.println("  Notified: " + workflow.getOutput().get("notified"));

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
