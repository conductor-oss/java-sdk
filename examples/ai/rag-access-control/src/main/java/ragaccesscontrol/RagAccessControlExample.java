package ragaccesscontrol;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragaccesscontrol.workers.AuthenticateUserWorker;
import ragaccesscontrol.workers.CheckPermissionsWorker;
import ragaccesscontrol.workers.FilteredRetrieveWorker;
import ragaccesscontrol.workers.RedactSensitiveWorker;
import ragaccesscontrol.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 163: RAG with Access Control
 *
 * Sequential pipeline that authenticates a user, checks permissions against
 * document collections, retrieves only allowed documents, redacts sensitive
 * fields, and generates an access-controlled answer.
 *
 * Run:
 *   java -jar target/rag-access-control-1.0.0.jar
 *   java -jar target/rag-access-control-1.0.0.jar --workers
 */
public class RagAccessControlExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 163: RAG with Access Control ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ac_authenticate_user", "ac_check_permissions", "ac_filtered_retrieve",
                "ac_redact_sensitive", "ac_generate"));
        System.out.println("  Registered: ac_authenticate_user, ac_check_permissions, ac_filtered_retrieve, ac_redact_sensitive, ac_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_access_control'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AuthenticateUserWorker(),
                new CheckPermissionsWorker(),
                new FilteredRetrieveWorker(),
                new RedactSensitiveWorker(),
                new GenerateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_access_control", 1, Map.of(
                "userId", "user-42",
                "authToken", "valid-jwt-token-12345",
                "question", "What are the engineering team guidelines?"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Authenticated: " + workflow.getOutput().get("authenticated"));
        System.out.println("  Allowed collections: " + workflow.getOutput().get("allowedCollections"));
        System.out.println("  Documents retrieved: " + workflow.getOutput().get("documentsRetrieved"));
        System.out.println("  After filter: " + workflow.getOutput().get("documentsAfterFilter"));
        System.out.println("  Fields redacted: " + workflow.getOutput().get("fieldsRedacted"));
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));

        System.out.println("\n--- RAG Access Control Pattern ---");
        System.out.println("  - Authenticate: Verify user identity and extract roles");
        System.out.println("  - Check permissions: Determine allowed/denied collections");
        System.out.println("  - Filtered retrieve: Only fetch from allowed collections");
        System.out.println("  - Redact sensitive: Remove SSNs, salary data based on clearance");
        System.out.println("  - Generate: Produce answer noting any redacted information");

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
