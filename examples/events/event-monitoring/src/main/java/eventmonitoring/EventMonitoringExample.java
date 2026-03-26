package eventmonitoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventmonitoring.workers.CollectMetricsWorker;
import eventmonitoring.workers.AnalyzeThroughputWorker;
import eventmonitoring.workers.AnalyzeLatencyWorker;
import eventmonitoring.workers.AnalyzeErrorsWorker;
import eventmonitoring.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Monitoring Demo
 *
 * Demonstrates a sequential event monitoring workflow:
 *   em_collect_metrics -> em_analyze_throughput -> em_analyze_latency
 *       -> em_analyze_errors -> em_generate_report
 *
 * Run:
 *   java -jar target/event-monitoring-1.0.0.jar
 */
public class EventMonitoringExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Monitoring Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "em_collect_metrics", "em_analyze_throughput",
                "em_analyze_latency", "em_analyze_errors", "em_generate_report"));
        System.out.println("  Registered: em_collect_metrics, em_analyze_throughput, em_analyze_latency, em_analyze_errors, em_generate_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_monitoring_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectMetricsWorker(),
                new AnalyzeThroughputWorker(),
                new AnalyzeLatencyWorker(),
                new AnalyzeErrorsWorker(),
                new GenerateReportWorker()
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
        String workflowId = client.startWorkflow("event_monitoring_wf", 1,
                Map.of("pipelineName", "order-events-pipeline",
                        "timeRange", "last_1h"));
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
