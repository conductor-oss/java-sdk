package modelserving;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import modelserving.workers.MsvLoadModelWorker;
import modelserving.workers.MsvValidateWorker;
import modelserving.workers.MsvDeployWorker;
import modelserving.workers.MsvTestWorker;
import modelserving.workers.MsvPromoteWorker;

import java.util.List;
import java.util.Map;

/**
 * Model Serving Demo
 *
 * Run:
 *   java -jar target/modelserving-1.0.0.jar
 */
public class ModelServingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Model Serving Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "msv_load_model",
                "msv_validate",
                "msv_deploy",
                "msv_test",
                "msv_promote"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'model_serving_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MsvLoadModelWorker(),
                new MsvValidateWorker(),
                new MsvDeployWorker(),
                new MsvTestWorker(),
                new MsvPromoteWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("model_serving_demo", 1,
                Map.of("modelName", "image-classifier", "modelVersion", "3.2", "modelPath", "/models/img_cls_v3.2.pt"));
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