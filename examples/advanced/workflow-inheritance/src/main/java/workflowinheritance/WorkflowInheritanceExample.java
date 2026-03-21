package workflowinheritance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowinheritance.workers.WiInitWorker;
import workflowinheritance.workers.WiValidateWorker;
import workflowinheritance.workers.WiProcessStandardWorker;
import workflowinheritance.workers.WiProcessPremiumWorker;
import workflowinheritance.workers.WiFinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Inheritance Demo
 *
 * Run:
 *   java -jar target/workflowinheritance-1.0.0.jar
 */
public class WorkflowInheritanceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Inheritance Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wi_init",
                "wi_validate",
                "wi_process_standard",
                "wi_process_premium",
                "wi_finalize"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'workflow_inheritance_standard_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WiInitWorker(),
                new WiValidateWorker(),
                new WiProcessStandardWorker(),
                new WiProcessPremiumWorker(),
                new WiFinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("workflow_inheritance_standard_demo", 1,
                Map.of("requestId", "REQ-STD-1", "data", "standard_payload"));
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