package conditionalapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import conditionalapproval.workers.ClassifyWorker;
import conditionalapproval.workers.ProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Conditional Approval Routing Based on Amount
 *
 * Routes approval requests to different approval chains based on the request amount:
 * - < $1,000:  Manager only (1 WAIT)
 * - $1,000 - $9,999: Manager + Director (2 WAITs)
 * - >= $10,000: Manager + Director + VP (3 WAITs)
 *
 * Run:
 *   java -jar target/conditional-approval-1.0.0.jar
 *   java -jar target/conditional-approval-1.0.0.jar --workers
 */
public class ConditionalApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Conditional Approval Routing Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef classifyTask = new TaskDef();
        classifyTask.setName("car_classify");
        classifyTask.setRetryCount(0);
        classifyTask.setTimeoutSeconds(60);
        classifyTask.setResponseTimeoutSeconds(30);
        classifyTask.setOwnerEmail("examples@orkes.io");

        TaskDef processTask = new TaskDef();
        processTask.setName("car_process");
        processTask.setRetryCount(0);
        processTask.setTimeoutSeconds(60);
        processTask.setResponseTimeoutSeconds(30);
        processTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(classifyTask, processTask));

        System.out.println("  Registered: car_classify, car_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'conditional_approval_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ClassifyWorker(), new ProcessWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run scenario: $500 request (low tier, manager only)
        System.out.println("Step 4: Starting workflow ($500 request - low tier)...\n");
        String workflowId = client.startWorkflow("conditional_approval_demo", 1,
                Map.of("requestId", "REQ-LO", "amount", 500, "requester", "emp@co.com"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion (WAIT tasks need manual completion in real use)
        System.out.println("Step 5: Waiting for completion...");
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
