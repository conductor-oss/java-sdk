package batchprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import batchprocessing.workers.PrepareBatchesWorker;
import batchprocessing.workers.ProcessBatchWorker;
import batchprocessing.workers.SummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Batch Processing Demo
 *
 * Demonstrates a DO_WHILE loop workflow:
 *   bp_prepare_batches -> DO_WHILE(bp_process_batch) -> bp_summarize
 *
 * Run:
 *   java -jar target/batch-processing-1.0.0.jar
 */
public class BatchProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Batch Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("bp_prepare_batches", "bp_process_batch", "bp_summarize"));
        System.out.println("  Registered: bp_prepare_batches, bp_process_batch, bp_summarize\n");

        System.out.println("Step 2: Registering workflow 'batch_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareBatchesWorker(),
                new ProcessBatchWorker(),
                new SummarizeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("batch_processing", 1,
                Map.of(
                        "records", List.of(
                                Map.of("id", 1, "data", "rec-1"),
                                Map.of("id", 2, "data", "rec-2"),
                                Map.of("id", 3, "data", "rec-3"),
                                Map.of("id", 4, "data", "rec-4"),
                                Map.of("id", 5, "data", "rec-5"),
                                Map.of("id", 6, "data", "rec-6"),
                                Map.of("id", 7, "data", "rec-7"),
                                Map.of("id", 8, "data", "rec-8"),
                                Map.of("id", 9, "data", "rec-9"),
                                Map.of("id", 10, "data", "rec-10")
                        ),
                        "batchSize", 3
                ));
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
