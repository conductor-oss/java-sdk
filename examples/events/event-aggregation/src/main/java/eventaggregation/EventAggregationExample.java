package eventaggregation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventaggregation.workers.CollectEventsWorker;
import eventaggregation.workers.AggregateMetricsWorker;
import eventaggregation.workers.GenerateSummaryWorker;
import eventaggregation.workers.PublishBatchWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Aggregation Demo
 *
 * Demonstrates a sequential pipeline of four workers that collect events,
 * aggregate metrics, generate a summary, and publish the batch:
 *   eg_collect_events -> eg_aggregate_metrics -> eg_generate_summary -> eg_publish_batch
 *
 * Run:
 *   java -jar target/event-aggregation-1.0.0.jar
 */
public class EventAggregationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Aggregation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "eg_collect_events", "eg_aggregate_metrics",
                "eg_generate_summary", "eg_publish_batch"));
        System.out.println("  Registered: eg_collect_events, eg_aggregate_metrics, eg_generate_summary, eg_publish_batch\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_aggregation_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectEventsWorker(),
                new AggregateMetricsWorker(),
                new GenerateSummaryWorker(),
                new PublishBatchWorker()
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
        String workflowId = client.startWorkflow("event_aggregation_wf", 1,
                Map.of("windowId", "win-fixed-001",
                        "windowDurationSec", 60,
                        "eventSource", "transaction-stream"));
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
