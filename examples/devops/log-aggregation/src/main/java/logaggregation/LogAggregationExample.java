package logaggregation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import logaggregation.workers.CollectLogs;
import logaggregation.workers.ParseLogs;
import logaggregation.workers.EnrichLogs;
import logaggregation.workers.StoreLogs;

import java.util.List;
import java.util.Map;

/**
 * Example 412: Log Aggregation
 *
 * Aggregate logs: collect raw logs, parse them into structured format,
 * enrich with metadata, and store in the log store.
 *
 * Pattern:
 *   collect -> parse -> enrich -> store
 *
 * Run:
 *   java -jar target/log-aggregation-1.0.0.jar
 */
public class LogAggregationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 412: Log Aggregation ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "la_collect_logs", "la_parse_logs", "la_enrich_logs", "la_store_logs"));
        System.out.println("  Registered: la_collect_logs, la_parse_logs, la_enrich_logs, la_store_logs\n");

        System.out.println("Step 2: Registering workflow 'log_aggregation_412'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectLogs(),
                new ParseLogs(),
                new EnrichLogs(),
                new StoreLogs()
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
        String workflowId = client.startWorkflow("log_aggregation_412", 1,
                Map.of("sources", List.of("api-gateway", "auth-service", "payment-service"),
                       "timeRange", "last-1h",
                       "logLevel", "INFO"));
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
