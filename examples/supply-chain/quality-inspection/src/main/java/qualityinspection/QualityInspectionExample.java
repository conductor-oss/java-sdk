package qualityinspection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import qualityinspection.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 656: Quality Inspection — Sample, Test, and Route Results
 */
public class QualityInspectionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 656: Quality Inspection ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of("qi_sample", "qi_test", "qi_handle_pass", "qi_handle_fail", "qi_record"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new SampleWorker(), new TestWorker(),
                new HandlePassWorker(), new HandleFailWorker(), new RecordWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("qi_quality_inspection", 1,
                Map.of("batchId", "BATCH-656-001", "product", "Precision Bearings", "sampleSize", 20));
        System.out.println("  Workflow ID: " + workflowId);

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
