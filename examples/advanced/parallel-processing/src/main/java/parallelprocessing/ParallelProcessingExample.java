package parallelprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import parallelprocessing.workers.PprSplitWorkWorker;
import parallelprocessing.workers.PprChunk1Worker;
import parallelprocessing.workers.PprChunk2Worker;
import parallelprocessing.workers.PprChunk3Worker;
import parallelprocessing.workers.PprMergeWorker;

import java.util.List;
import java.util.Map;

/**
 * Parallel Processing Demo
 *
 * Run:
 *   java -jar target/parallelprocessing-1.0.0.jar
 */
public class ParallelProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Parallel Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ppr_split_work",
                "ppr_chunk_1",
                "ppr_chunk_2",
                "ppr_chunk_3",
                "ppr_merge"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'ppr_parallel_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PprSplitWorkWorker(),
                new PprChunk1Worker(),
                new PprChunk2Worker(),
                new PprChunk3Worker(),
                new PprMergeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("ppr_parallel_processing", 1,
                Map.of("dataset", java.util.List.of(1,2,3,4,5,6,7,8,9), "chunkSize", 3));
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