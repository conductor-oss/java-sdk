package datasampling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datasampling.workers.LoadDatasetWorker;
import datasampling.workers.DrawSampleWorker;
import datasampling.workers.RunQualityChecksWorker;
import datasampling.workers.ApproveDatasetWorker;
import datasampling.workers.FlagForReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Sampling Workflow Demo
 *
 * Demonstrates a Sequential+SWITCH workflow:
 *   sm_load_dataset -> sm_draw_sample -> sm_run_quality_checks -> SWITCH(decision:
 *       pass    -> sm_approve_dataset,
 *       default -> sm_flag_for_review)
 *
 * Run:
 *   java -jar target/data-sampling-1.0.0.jar
 */
public class DataSamplingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Sampling Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sm_load_dataset", "sm_draw_sample",
                "sm_run_quality_checks", "sm_approve_dataset", "sm_flag_for_review"));
        System.out.println("  Registered: sm_load_dataset, sm_draw_sample, sm_run_quality_checks, sm_approve_dataset, sm_flag_for_review\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'data_sampling_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadDatasetWorker(),
                new DrawSampleWorker(),
                new RunQualityChecksWorker(),
                new ApproveDatasetWorker(),
                new FlagForReviewWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_sampling_wf", 1,
                Map.of("records", List.of(
                                Map.of("name", "Alice", "value", 95),
                                Map.of("name", "Bob", "value", 82),
                                Map.of("name", "Charlie", "value", 77),
                                Map.of("name", "Diana", "value", 90)),
                        "sampleRate", 0.5,
                        "threshold", 0.8));
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
