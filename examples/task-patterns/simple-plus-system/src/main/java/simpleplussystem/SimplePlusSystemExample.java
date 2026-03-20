package simpleplussystem;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import simpleplussystem.workers.FetchOrdersWorker;
import simpleplussystem.workers.GenerateVisualReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Combining SIMPLE Tasks with System Tasks
 *
 * Real workflows often mix SIMPLE tasks (with workers) and system tasks:
 * - SIMPLE: Complex business logic, external API calls, ML inference
 * - INLINE: Quick calculations, data formatting (runs on the server, no worker needed)
 *
 * Pipeline:
 *   [SIMPLE] fetch_orders -> [INLINE] calculate_stats ->
 *   [SIMPLE] generate_visual_report -> [INLINE] format_summary
 *
 * Run:
 *   java -jar target/simple-plus-system-1.0.0.jar
 */
public class SimplePlusSystemExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 14: SIMPLE + System Tasks ===\n");
        System.out.println("SIMPLE tasks  -> run on YOUR workers (complex logic)");
        System.out.println("INLINE tasks  -> run on the SERVER (quick calculations)\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions (only for SIMPLE tasks)
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("fetch_orders", "generate_visual_report"));
        System.out.println("  Registered: fetch_orders, generate_visual_report");
        System.out.println("  (INLINE tasks need no registration)\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'mixed_tasks_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers (only for SIMPLE tasks)
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FetchOrdersWorker(),
                new GenerateVisualReportWorker()
        );
        client.startWorkers(workers);
        System.out.println("  2 workers polling (INLINE tasks run on server).\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("mixed_tasks_demo", 1,
                Map.of("storeId", "STORE-42", "dateRange", "2026-Q1"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        // Show which tasks ran where
        System.out.println("\nTask execution locations:");
        if (workflow.getTasks() != null) {
            workflow.getTasks().forEach(t -> {
                String type = "SIMPLE".equals(t.getTaskType()) ? "WORKER" : "SERVER";
                System.out.println("  " + t.getReferenceTaskName() + ": [" + type + "] " + t.getStatus());
            });
        }

        System.out.println("\n  Output: " + workflow.getOutput());

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
