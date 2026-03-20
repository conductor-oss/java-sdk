package modelregistry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import modelregistry.workers.MrgRegisterWorker;
import modelregistry.workers.MrgVersionWorker;
import modelregistry.workers.MrgValidateWorker;
import modelregistry.workers.MrgApproveWorker;
import modelregistry.workers.MrgDeployWorker;

import java.util.List;
import java.util.Map;

/**
 * Model Registry Demo
 *
 * Run:
 *   java -jar target/modelregistry-1.0.0.jar
 */
public class ModelRegistryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Model Registry Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mrg_register",
                "mrg_version",
                "mrg_validate",
                "mrg_approve",
                "mrg_deploy"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'model_registry_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MrgRegisterWorker(),
                new MrgVersionWorker(),
                new MrgValidateWorker(),
                new MrgApproveWorker(),
                new MrgDeployWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("model_registry_demo", 1,
                Map.of("modelName", "fraud-detector", "modelArtifact", "s3://ml-artifacts/fraud-detector/v3.1.0.tar.gz"));
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