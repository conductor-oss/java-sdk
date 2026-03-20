package orderedprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import orderedprocessing.workers.OprReceiveWorker;
import orderedprocessing.workers.OprSortBySequenceWorker;
import orderedprocessing.workers.OprProcessInOrderWorker;
import orderedprocessing.workers.OprVerifyOrderWorker;

import java.util.List;
import java.util.Map;

/**
 * Ordered Processing Demo
 *
 * Run:
 *   java -jar target/orderedprocessing-1.0.0.jar
 */
public class OrderedProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Ordered Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "opr_receive",
                "opr_sort_by_sequence",
                "opr_process_in_order",
                "opr_verify_order"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'opr_ordered_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new OprReceiveWorker(),
                new OprSortBySequenceWorker(),
                new OprProcessInOrderWorker(),
                new OprVerifyOrderWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("opr_ordered_processing", 1,
                Map.of("messages", java.util.List.of(java.util.Map.of("seq",3,"data","third"), java.util.Map.of("seq",1,"data","first")), "partitionKey", "user-123"));
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