package trainingdatalabeling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import trainingdatalabeling.workers.ComputeAgreementWorker;
import trainingdatalabeling.workers.PrepareBatchWorker;
import trainingdatalabeling.workers.StoreLabelsWorker;

import java.util.List;
import java.util.Map;

/**
 * Training Data Labeling -- Parallel Annotators with Agreement Computation
 *
 * Demonstrates a human-in-the-loop workflow where:
 * 1. A batch of data is prepared for labeling
 * 2. Two annotators independently label the data (WAIT tasks in FORK_JOIN)
 * 3. Inter-annotator agreement is computed
 * 4. Final labels are stored
 *
 * Run:
 *   java -jar target/training-data-labeling-1.0.0.jar
 *   java -jar target/training-data-labeling-1.0.0.jar --workers
 */
public class TrainingDataLabelingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Training Data Labeling: Parallel Annotators with Agreement ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareBatchTask = new TaskDef();
        prepareBatchTask.setName("tdl_prepare_batch");
        prepareBatchTask.setTimeoutSeconds(60);
        prepareBatchTask.setResponseTimeoutSeconds(30);
        prepareBatchTask.setOwnerEmail("examples@orkes.io");

        TaskDef computeAgreementTask = new TaskDef();
        computeAgreementTask.setName("tdl_compute_agreement");
        computeAgreementTask.setTimeoutSeconds(60);
        computeAgreementTask.setResponseTimeoutSeconds(30);
        computeAgreementTask.setOwnerEmail("examples@orkes.io");

        TaskDef storeLabelsTask = new TaskDef();
        storeLabelsTask.setName("tdl_store_labels");
        storeLabelsTask.setTimeoutSeconds(60);
        storeLabelsTask.setResponseTimeoutSeconds(30);
        storeLabelsTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareBatchTask, computeAgreementTask, storeLabelsTask));

        System.out.println("  Registered: tdl_prepare_batch, tdl_compute_agreement, tdl_store_labels\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'training_data_labeling'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareBatchWorker(),
                new ComputeAgreementWorker(),
                new StoreLabelsWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("training_data_labeling", 1,
                Map.of("batchId", "batch-001"));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  Waiting for annotators (WAIT tasks)...\n");

        // In a real scenario, annotators would complete WAIT tasks via API
        // For demo purposes, wait and then show status
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
