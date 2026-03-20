package batchmltraining;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import batchmltraining.workers.BmlPrepareDataWorker;
import batchmltraining.workers.BmlSplitDataWorker;
import batchmltraining.workers.BmlTrainModel1Worker;
import batchmltraining.workers.BmlTrainModel2Worker;
import batchmltraining.workers.BmlEvaluateWorker;

import java.util.List;
import java.util.Map;

/**
 * Batch ML Training Demo
 *
 * Run:
 *   java -jar target/batchmltraining-1.0.0.jar
 */
public class BatchMlTrainingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Batch ML Training Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "bml_prepare_data",
                "bml_split_data",
                "bml_train_model_1",
                "bml_train_model_2",
                "bml_evaluate"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'batch_ml_training_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BmlPrepareDataWorker(),
                new BmlSplitDataWorker(),
                new BmlTrainModel1Worker(),
                new BmlTrainModel2Worker(),
                new BmlEvaluateWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("batch_ml_training_demo", 1,
                Map.of("datasetId", "DS-CHURN-2024", "experimentName", "churn-prediction-v3"));
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