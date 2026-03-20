package eventwindowing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventwindowing.workers.CollectWindowWorker;
import eventwindowing.workers.ComputeStatsWorker;
import eventwindowing.workers.EmitResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Windowing Demo
 *
 * Demonstrates a sequential pipeline of three workers that process a stream of
 * timestamped events through a fixed time window:
 * collect events into window, compute aggregate statistics, and emit the result.
 *   ew_collect_window -> ew_compute_stats -> ew_emit_result
 *
 * Run:
 *   java -jar target/event-windowing-1.0.0.jar
 */
public class EventWindowingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Windowing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ew_collect_window", "ew_compute_stats", "ew_emit_result"));
        System.out.println("  Registered: ew_collect_window, ew_compute_stats, ew_emit_result\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'event_windowing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectWindowWorker(),
                new ComputeStatsWorker(),
                new EmitResultWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_windowing", 1,
                Map.of("events", List.of(
                                Map.of("ts", 1000, "value", 10),
                                Map.of("ts", 1500, "value", 25),
                                Map.of("ts", 2000, "value", 15),
                                Map.of("ts", 2500, "value", 30),
                                Map.of("ts", 3000, "value", 20)),
                        "windowSizeMs", 5000));
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
