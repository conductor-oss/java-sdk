package complexeventprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import complexeventprocessing.workers.IngestEventsWorker;
import complexeventprocessing.workers.DetectSequenceWorker;
import complexeventprocessing.workers.DetectAbsenceWorker;
import complexeventprocessing.workers.DetectTimingWorker;
import complexeventprocessing.workers.TriggerAlertWorker;
import complexeventprocessing.workers.LogNormalWorker;

import java.util.List;
import java.util.Map;

/**
 * Complex Event Processing Demo
 *
 * Demonstrates a Sequential+SWITCH pattern for complex event processing:
 *   cp_ingest_events -> cp_detect_sequence -> cp_detect_absence -> cp_detect_timing
 *   -> SWITCH(pattern_found -> cp_trigger_alert, default -> cp_log_normal)
 *
 * Run:
 *   java -jar target/complex-event-processing-1.0.0.jar
 */
public class ComplexEventProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Complex Event Processing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cp_ingest_events", "cp_detect_sequence", "cp_detect_absence",
                "cp_detect_timing", "cp_trigger_alert", "cp_log_normal"));
        System.out.println("  Registered: cp_ingest_events, cp_detect_sequence, cp_detect_absence, cp_detect_timing, cp_trigger_alert, cp_log_normal\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'complex_event_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestEventsWorker(),
                new DetectSequenceWorker(),
                new DetectAbsenceWorker(),
                new DetectTimingWorker(),
                new TriggerAlertWorker(),
                new LogNormalWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("complex_event_processing", 1,
                Map.of("events", List.of(
                        Map.of("type", "login", "ts", 1000, "userId", "U-50"),
                        Map.of("type", "browse", "ts", 1500, "userId", "U-50"),
                        Map.of("type", "purchase", "ts", 8000, "userId", "U-50"),
                        Map.of("type", "logout", "ts", 9000, "userId", "U-50")
                ),
                "patternRules", List.of("login_then_purchase", "expected_confirmation")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
