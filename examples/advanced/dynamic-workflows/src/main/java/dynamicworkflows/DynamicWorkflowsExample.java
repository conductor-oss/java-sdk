package dynamicworkflows;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dynamicworkflows.workers.DwValidateWorker;
import dynamicworkflows.workers.DwTransformWorker;
import dynamicworkflows.workers.DwEnrichWorker;
import dynamicworkflows.workers.DwPublishWorker;

import java.util.List;
import java.util.Map;

/**
 * Dynamic Workflows Demo
 *
 * Build a workflow definition dynamically from a configuration object,
 * register it, and execute it.
 *
 * Run:
 *   java -jar target/dynamic-workflows-1.0.0.jar
 */
public class DynamicWorkflowsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Dynamic Workflows Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("dw_validate", "dw_transform", "dw_enrich", "dw_publish"));
        System.out.println("  Registered: dw_validate, dw_transform, dw_enrich, dw_publish\n");

        System.out.println("Step 2: Registering workflow 'dynamic_workflow_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DwValidateWorker(),
                new DwTransformWorker(),
                new DwEnrichWorker(),
                new DwPublishWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("dynamic_workflow_demo", 1,
                Map.of("pipelineName", "dynamic_pipeline",
                        "payload", Map.of("data", "test_record_1")));
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
