package gcpintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gcpintegration.workers.GcpGcsUploadWorker;
import gcpintegration.workers.GcpFirestoreWriteWorker;
import gcpintegration.workers.GcpPubsubPublishWorker;
import gcpintegration.workers.GcpVerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * GCP Integration Demo
 *
 * Performs a GCP multi-service integration workflow using FORK_JOIN:
 *   FORK(GCS upload, Firestore write, PubSub publish) -> JOIN -> verify.
 */
public class GcpIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 442: GCP Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gcp_gcs_upload", "gcp_firestore_write", "gcp_pubsub_publish", "gcp_verify"));
        System.out.println("  Registered: gcp_gcs_upload, gcp_firestore_write, gcp_pubsub_publish, gcp_verify\n");

        System.out.println("Step 2: Registering workflow 'gcp_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GcpGcsUploadWorker(),
                new GcpFirestoreWriteWorker(),
                new GcpPubsubPublishWorker(),
                new GcpVerifyWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("gcp_integration", 1,
                Map.of("bucketName", "my-gcp-data-bucket",
                        "collection", "events",
                        "topicName", "projects/my-proj/topics/data-events",
                        "payload", Map.of(
                                "id", "evt-6001",
                                "type", "user.signup",
                                "timestamp", "2026-01-15T10:00:00Z",
                                "data", Map.of("userId", "usr-321", "plan", "pro"))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
