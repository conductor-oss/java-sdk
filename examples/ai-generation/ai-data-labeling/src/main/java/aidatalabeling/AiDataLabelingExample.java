package aidatalabeling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aidatalabeling.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 805: AI Data Labeling — Prepare Data, FORK(Labeler 1, Labeler 2), JOIN, Reconcile, Export
 *
 * Runs an AI data labeling pipeline with parallel labelers via FORK/JOIN.
 */
public class AiDataLabelingExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 805: AI Data Labeling ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("adl_prepare_data", "adl_labeler_1", "adl_labeler_2", "adl_reconcile", "adl_export"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(
                new PrepareDataWorker(),
                new Labeler1Worker(),
                new Labeler2Worker(),
                new ReconcileWorker(),
                new ExportWorker()
        );
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("adl_data_labeling", 1, Map.of(
                    "datasetId", "DS-805",
                    "labelType", "image-classification"
            ));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Total labeled: %s%n", workflow.getOutput().get("totalLabeled"));
            System.out.printf("Agreement: %s%n", workflow.getOutput().get("agreement"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
