package cdcpipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cdcpipeline.workers.DetectChangesWorker;
import cdcpipeline.workers.TransformChangesWorker;
import cdcpipeline.workers.PublishDownstreamWorker;
import cdcpipeline.workers.ConfirmDeliveryWorker;

import java.util.List;
import java.util.Map;

/**
 * CDC Pipeline Demo
 *
 * Demonstrates a sequential pipeline of four workers that capture, transform,
 * publish, and confirm delivery of change-data-capture events:
 *   cd_detect_changes -> cd_transform_changes -> cd_publish_downstream -> cd_confirm_delivery
 *
 * Run:
 *   java -jar target/cdc-pipeline-1.0.0.jar
 */
public class CdcPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== CDC Pipeline Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cd_detect_changes", "cd_transform_changes",
                "cd_publish_downstream", "cd_confirm_delivery"));
        System.out.println("  Registered: cd_detect_changes, cd_transform_changes, cd_publish_downstream, cd_confirm_delivery\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'cdc_pipeline_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectChangesWorker(),
                new TransformChangesWorker(),
                new PublishDownstreamWorker(),
                new ConfirmDeliveryWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cdc_pipeline_wf", 1,
                Map.of("sourceTable", "users",
                        "sinceTimestamp", "2026-03-08T10:00:00Z",
                        "targetTopic", "cdc.users.changes"));
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
