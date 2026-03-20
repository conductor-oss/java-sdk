package datadedup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datadedup.workers.LoadRecordsWorker;
import datadedup.workers.ComputeKeysWorker;
import datadedup.workers.FindDuplicatesWorker;
import datadedup.workers.MergeGroupsWorker;
import datadedup.workers.EmitDedupedWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Deduplication Workflow Demo
 *
 * Demonstrates a sequential deduplication pipeline:
 *   dp_load_records -> dp_compute_keys -> dp_find_duplicates -> dp_merge_groups -> dp_emit_deduped
 *
 * Run:
 *   java -jar target/data-dedup-1.0.0.jar
 */
public class DataDedupExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Deduplication Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dp_load_records", "dp_compute_keys",
                "dp_find_duplicates", "dp_merge_groups", "dp_emit_deduped"));
        System.out.println("  Registered: dp_load_records, dp_compute_keys, dp_find_duplicates, dp_merge_groups, dp_emit_deduped\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_dedup'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadRecordsWorker(),
                new ComputeKeysWorker(),
                new FindDuplicatesWorker(),
                new MergeGroupsWorker(),
                new EmitDedupedWorker()
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
        String workflowId = client.startWorkflow("data_dedup", 1,
                Map.of("records", List.of(
                                Map.of("id", 1, "name", "Alice Smith", "email", "alice@example.com", "phone", "555-0101"),
                                Map.of("id", 2, "name", "Bob Jones", "email", "bob@example.com", "phone", "555-0102"),
                                Map.of("id", 3, "name", "Alice S.", "email", "alice@example.com", "phone", "555-0101"),
                                Map.of("id", 4, "name", "Charlie Brown", "email", "charlie@example.com", "phone", "555-0103"),
                                Map.of("id", 5, "name", "Bob J.", "email", "bob@example.com", "phone", "555-0102"),
                                Map.of("id", 6, "name", "Diana Prince", "email", "diana@example.com", "phone", "555-0104")),
                        "matchFields", List.of("email")));
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
