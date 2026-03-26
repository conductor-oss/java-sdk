package incrementalrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import incrementalrag.workers.DetectChangesWorker;
import incrementalrag.workers.FilterNewDocsWorker;
import incrementalrag.workers.EmbedIncrementalWorker;
import incrementalrag.workers.UpsertVectorsWorker;
import incrementalrag.workers.VerifyIndexWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 161: Incremental RAG
 *
 * Detects changed documents, filters new vs updated, embeds incrementally,
 * upserts vectors, and verifies the index in a sequential pipeline.
 *
 * Run:
 *   java -jar target/incremental-rag-1.0.0.jar
 *   java -jar target/incremental-rag-1.0.0.jar --workers
 */
public class IncrementalRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 161: Incremental RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ir_detect_changes", "ir_filter_new_docs", "ir_embed_incremental",
                "ir_upsert_vectors", "ir_verify_index"));
        System.out.println("  Registered: ir_detect_changes, ir_filter_new_docs, ir_embed_incremental, ir_upsert_vectors, ir_verify_index\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'incremental_rag'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectChangesWorker(),
                new FilterNewDocsWorker(),
                new EmbedIncrementalWorker(),
                new UpsertVectorsWorker(),
                new VerifyIndexWorker()
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
        String workflowId = client.startWorkflow("incremental_rag", 1, Map.of(
                "sourceCollection", "knowledge_base",
                "lastSyncTimestamp", "2025-01-01T00:00:00Z"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Total changed: " + workflow.getOutput().get("totalChanged"));
        System.out.println("  New: " + workflow.getOutput().get("newCount"));
        System.out.println("  Updated: " + workflow.getOutput().get("updatedCount"));
        System.out.println("  Embedded: " + workflow.getOutput().get("embeddedCount"));
        System.out.println("  Upserted: " + workflow.getOutput().get("upsertedCount"));
        System.out.println("  Verified: " + workflow.getOutput().get("verified"));
        System.out.println("  Vector count: " + workflow.getOutput().get("vectorCount"));

        System.out.println("\n--- Incremental RAG Pattern ---");
        System.out.println("  - Detect changes: Find documents modified since last sync");
        System.out.println("  - Filter new docs: Separate new vs updated documents");
        System.out.println("  - Embed incremental: Generate embeddings only for changed docs");
        System.out.println("  - Upsert vectors: Insert or update vectors in the store");
        System.out.println("  - Verify index: Confirm index integrity and measure latency");

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
