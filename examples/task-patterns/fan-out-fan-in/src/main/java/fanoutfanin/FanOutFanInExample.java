package fanoutfanin;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import fanoutfanin.workers.PrepareWorker;
import fanoutfanin.workers.ProcessImageWorker;
import fanoutfanin.workers.AggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Fan-Out/Fan-In — Scatter-gather pattern using FORK_JOIN_DYNAMIC
 *
 * Demonstrates the fan-out/fan-in pattern in Conductor:
 * a prepare worker splits an image list into parallel tasks,
 * each image is processed independently, then results are aggregated.
 *
 * Run:
 *   java -jar target/fan-out-fan-in-1.0.0.jar
 */
public class FanOutFanInExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Fan-Out/Fan-In: Parallel Image Processing ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("fo_prepare", "fo_process_image", "fo_aggregate"));
        System.out.println("  Registered: fo_prepare, fo_process_image, fo_aggregate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'fan_out_fan_in_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareWorker(),
                new ProcessImageWorker(),
                new AggregateWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("fan_out_fan_in_demo", 1,
                Map.of("images", List.of(
                        Map.of("name", "hero.jpg", "size", 2400),
                        Map.of("name", "banner.png", "size", 3600),
                        Map.of("name", "thumb.jpg", "size", 800)
                )));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
