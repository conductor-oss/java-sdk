package dataclassification;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataclassification.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Automated Data Sensitivity Classification
 *
 * Pattern: scan-data-stores -> detect-pii -> classify -> apply-labels
 */
public class DataClassificationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Classification Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "dc_scan_data_stores", "dc_detect_pii", "dc_classify", "dc_apply_labels"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ScanDataStoresWorker(), new DetectPiiWorker(),
                new ClassifyWorker(), new ApplyLabelsWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }

        Thread.sleep(2000);
        String workflowId = client.startWorkflow("data_classification_workflow", 1,
                Map.of("dataStore", "customer-database", "scanType", "full"));

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
