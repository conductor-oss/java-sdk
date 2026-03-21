package workflowcomposition;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowcomposition.workers.WcpSubAStep1Worker;
import workflowcomposition.workers.WcpSubAStep2Worker;
import workflowcomposition.workers.WcpSubBStep1Worker;
import workflowcomposition.workers.WcpSubBStep2Worker;
import workflowcomposition.workers.WcpMergeWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Composition Demo
 *
 * Run:
 *   java -jar target/workflowcomposition-1.0.0.jar
 */
public class WorkflowCompositionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Composition Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wcp_sub_a_step1",
                "wcp_sub_a_step2",
                "wcp_sub_b_step1",
                "wcp_sub_b_step2",
                "wcp_merge"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'workflow_composition_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WcpSubAStep1Worker(),
                new WcpSubAStep2Worker(),
                new WcpSubBStep1Worker(),
                new WcpSubBStep2Worker(),
                new WcpMergeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("workflow_composition_demo", 1,
                Map.of("orderId", "ORD-9001", "customerId", "CUST-42"));
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