package mapreduce;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import mapreduce.workers.MapWorker;
import mapreduce.workers.AnalyzeLogWorker;
import mapreduce.workers.ReduceWorker;

import java.util.List;
import java.util.Map;

/**
 * MapReduce Pattern — Classic MapReduce with Dynamic FORK and JOIN
 *
 * Demonstrates the MapReduce pattern using Conductor:
 * MAP: Split log files into parallel analysis tasks
 * Each worker analyzes one log file (count errors, warnings)
 * REDUCE: Aggregate all counts into summary report
 *
 * Run:
 *   java -jar target/map-reduce-1.0.0.jar
 */
public class MapReduceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== MapReduce: Log File Analysis ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("mr_map", "mr_analyze_log", "mr_reduce"));
        System.out.println("  Registered: mr_map, mr_analyze_log, mr_reduce\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'map_reduce_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MapWorker(),
                new AnalyzeLogWorker(),
                new ReduceWorker()
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
        String workflowId = client.startWorkflow("map_reduce_demo", 1,
                Map.of("logFiles", List.of(
                        Map.of("name", "api-server.log", "lineCount", 50000),
                        Map.of("name", "auth-service.log", "lineCount", 30000),
                        Map.of("name", "payment-gateway.log", "lineCount", 75000),
                        Map.of("name", "notification-service.log", "lineCount", 20000)
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
