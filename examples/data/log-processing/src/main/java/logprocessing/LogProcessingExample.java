package logprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import logprocessing.workers.IngestLogsWorker;
import logprocessing.workers.ParseEntriesWorker;
import logprocessing.workers.ExtractPatternsWorker;
import logprocessing.workers.AggregateMetricsWorker;

import java.util.List;
import java.util.Map;

/**
 * Log Processing Workflow Demo
 *
 * Demonstrates a log processing pipeline:
 *   lp_ingest_logs -> lp_parse_entries -> lp_extract_patterns -> lp_aggregate_metrics
 *
 * Run:
 *   java -jar target/log-processing-1.0.0.jar
 */
public class LogProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Log Processing Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lp_ingest_logs", "lp_parse_entries",
                "lp_extract_patterns", "lp_aggregate_metrics"));
        System.out.println("  Registered: lp_ingest_logs, lp_parse_entries, lp_extract_patterns, lp_aggregate_metrics\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'log_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestLogsWorker(),
                new ParseEntriesWorker(),
                new ExtractPatternsWorker(),
                new AggregateMetricsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("log_processing", 1,
                Map.of("logSource", "prod-cluster-east",
                        "timeRange", Map.of("start", "2024-03-15T10:00:00Z", "end", "2024-03-15T10:05:00Z"),
                        "filters", Map.of("minLevel", "INFO")));
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
