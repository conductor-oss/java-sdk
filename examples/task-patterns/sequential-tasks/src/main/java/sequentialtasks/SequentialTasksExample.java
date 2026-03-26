package sequentialtasks;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sequentialtasks.workers.ExtractWorker;
import sequentialtasks.workers.TransformWorker;
import sequentialtasks.workers.LoadWorker;

import java.util.List;
import java.util.Map;

/**
 * Sequential ETL Pipeline — Extract, Transform, Load
 *
 * Demonstrates sequential task execution in Conductor:
 * three workers process data in order, each consuming the
 * previous task's output.
 *
 * Run:
 *   java -jar target/sequential-tasks-1.0.0.jar
 */
public class SequentialTasksExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Sequential ETL Pipeline: Extract -> Transform -> Load ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("seq_extract", "seq_transform", "seq_load"));
        System.out.println("  Registered: seq_extract, seq_transform, seq_load\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'sequential_etl'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtractWorker(),
                new TransformWorker(),
                new LoadWorker()
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
        String workflowId = client.startWorkflow("sequential_etl", 1,
                Map.of("source", "user_database", "format", "enriched"));
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
