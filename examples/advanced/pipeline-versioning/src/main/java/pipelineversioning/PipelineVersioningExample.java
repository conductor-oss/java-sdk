package pipelineversioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import pipelineversioning.workers.PvrSnapshotConfigWorker;
import pipelineversioning.workers.PvrTagVersionWorker;
import pipelineversioning.workers.PvrTestPipelineWorker;
import pipelineversioning.workers.PvrPromoteWorker;

import java.util.List;
import java.util.Map;

/**
 * Pipeline Versioning Demo
 *
 * Run:
 *   java -jar target/pipelineversioning-1.0.0.jar
 */
public class PipelineVersioningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Pipeline Versioning Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pvr_snapshot_config",
                "pvr_tag_version",
                "pvr_test_pipeline",
                "pvr_promote"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'pipeline_versioning_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PvrSnapshotConfigWorker(),
                new PvrTagVersionWorker(),
                new PvrTestPipelineWorker(),
                new PvrPromoteWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("pipeline_versioning_demo", 1,
                Map.of("pipelineName", "etl-orders", "versionTag", "v4.2.0", "environment", "production"));
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