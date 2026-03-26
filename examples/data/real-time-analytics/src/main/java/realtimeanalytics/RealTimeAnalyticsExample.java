package realtimeanalytics;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import realtimeanalytics.workers.IngestEventsWorker;
import realtimeanalytics.workers.ProcessStreamWorker;
import realtimeanalytics.workers.UpdateAggregatesWorker;
import realtimeanalytics.workers.CheckAlertsWorker;

import java.util.List;
import java.util.Map;

/**
 * Real-Time Analytics Workflow Demo
 *
 * Demonstrates a real-time analytics pipeline:
 *   ry_ingest_events -> ry_process_stream -> ry_update_aggregates -> ry_check_alerts
 *
 * Run:
 *   java -jar target/real-time-analytics-1.0.0.jar
 */
public class RealTimeAnalyticsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Real-Time Analytics Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ry_ingest_events", "ry_process_stream",
                "ry_update_aggregates", "ry_check_alerts"));
        System.out.println("  Registered: ry_ingest_events, ry_process_stream, ry_update_aggregates, ry_check_alerts\n");

        System.out.println("Step 2: Registering workflow 'real_time_analytics'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestEventsWorker(),
                new ProcessStreamWorker(),
                new UpdateAggregatesWorker(),
                new CheckAlertsWorker()
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
        String workflowId = client.startWorkflow("real_time_analytics", 1,
                Map.of("eventBatch", Map.of("batchId", "B-20240315-001", "source", "web-app"),
                        "windowSize", 60,
                        "alertRules", List.of(
                                Map.of("metric", "anomaly_count", "operator", "gt", "threshold", 1),
                                Map.of("metric", "error_count", "operator", "gt", "threshold", 0),
                                Map.of("metric", "p99_latency", "operator", "gt", "threshold", 500))));
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
