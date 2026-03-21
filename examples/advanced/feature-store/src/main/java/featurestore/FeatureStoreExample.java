package featurestore;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import featurestore.workers.FstComputeFeaturesWorker;
import featurestore.workers.FstValidateWorker;
import featurestore.workers.FstRegisterWorker;
import featurestore.workers.FstServeWorker;

import java.util.List;
import java.util.Map;

/**
 * Feature Store Demo
 *
 * Run:
 *   java -jar target/featurestore-1.0.0.jar
 */
public class FeatureStoreExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Feature Store Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fst_compute_features",
                "fst_validate",
                "fst_register",
                "fst_serve"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'feature_store_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FstComputeFeaturesWorker(),
                new FstValidateWorker(),
                new FstRegisterWorker(),
                new FstServeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("feature_store_demo", 1,
                Map.of("featureGroupName", "user_behavior", "sourceTable", "events_raw", "entityKey", "user_id"));
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