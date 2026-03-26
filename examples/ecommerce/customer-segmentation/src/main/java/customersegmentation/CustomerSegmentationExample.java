package customersegmentation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import customersegmentation.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 464: Customer Segmentation
 *
 * Collect customer data, run clustering, label segments, and target with campaigns.
 */
public class CustomerSegmentationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 464: Customer Segmentation ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("seg_collect_data", "seg_cluster", "seg_label_segments", "seg_target"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CollectDataWorker(), new ClusterWorker(), new LabelSegmentsWorker(), new TargetWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("customer_segmentation_workflow", 1, Map.of("datasetId", "DS-2024Q1", "numSegments", 3));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
