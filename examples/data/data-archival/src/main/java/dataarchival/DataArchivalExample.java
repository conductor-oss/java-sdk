package dataarchival;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataarchival.workers.IdentifyStaleWorker;
import dataarchival.workers.SnapshotRecordsWorker;
import dataarchival.workers.TransferToColdWorker;
import dataarchival.workers.VerifyArchiveWorker;
import dataarchival.workers.PurgeHotWorker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Data Archival Workflow Demo
 *
 * Demonstrates a data archival pipeline:
 *   arc_identify_stale -> arc_snapshot_records -> arc_transfer_to_cold -> arc_verify_archive -> arc_purge_hot
 *
 * Run:
 *   java -jar target/data-archival-1.0.0.jar
 */
public class DataArchivalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Archival Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "arc_identify_stale", "arc_snapshot_records", "arc_transfer_to_cold",
                "arc_verify_archive", "arc_purge_hot"));
        System.out.println("  Registered: arc_identify_stale, arc_snapshot_records, arc_transfer_to_cold, arc_verify_archive, arc_purge_hot\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_archival'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IdentifyStaleWorker(),
                new SnapshotRecordsWorker(),
                new TransferToColdWorker(),
                new VerifyArchiveWorker(),
                new PurgeHotWorker()
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
        Instant now = Instant.now();
        String workflowId = client.startWorkflow("data_archival", 1,
                Map.of("records", List.of(
                        Map.of("id", "R-001", "name", "Old Report Q1", "lastAccessed", "2024-01-15T00:00:00Z", "sizeKb", 450),
                        Map.of("id", "R-002", "name", "Old Report Q2", "lastAccessed", "2024-03-20T00:00:00Z", "sizeKb", 300),
                        Map.of("id", "R-003", "name", "Recent Report", "lastAccessed", now.minus(30, ChronoUnit.DAYS).toString(), "sizeKb", 520),
                        Map.of("id", "R-004", "name", "Old Logs 2023", "lastAccessed", "2023-11-01T00:00:00Z", "sizeKb", 1200),
                        Map.of("id", "R-005", "name", "Current Data", "lastAccessed", now.minus(10, ChronoUnit.DAYS).toString(), "sizeKb", 890),
                        Map.of("id", "R-006", "name", "Archived Invoice", "lastAccessed", "2024-02-28T00:00:00Z", "sizeKb", 150)
                ),
                "retentionDays", 90,
                "coldStoragePath", "s3://company-archive/data"));
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
