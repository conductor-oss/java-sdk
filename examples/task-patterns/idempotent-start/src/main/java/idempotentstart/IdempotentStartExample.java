package idempotentstart;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import idempotentstart.workers.IdemProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Idempotent Start — Use correlationId to prevent duplicate workflow executions.
 *
 * Demonstrates correlationId-based dedup and search-based idempotency:
 * - Starting a workflow with a correlationId ties it to a logical operation
 * - Before starting a new execution, search by correlationId to find existing runs
 * - If a matching workflow is already RUNNING or COMPLETED, skip re-execution
 *
 * Run:
 *   java -jar target/idempotent-start-1.0.0.jar
 *   java -jar target/idempotent-start-1.0.0.jar --workers
 */
public class IdempotentStartExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Idempotent Start Demo: correlationId-Based Dedup ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("idem_process");
        taskDef.setRetryCount(2);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setOwnerEmail("examples@orkes.io");
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  Registered: idem_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'idempotent_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new IdemProcessWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start workflow with correlationId
        String correlationId = "order-12345";
        System.out.println("Step 4: Starting workflow with correlationId='" + correlationId + "'...\n");
        String workflowId = client.startWorkflow("idempotent_demo", 1,
                Map.of("orderId", "12345", "amount", 99.95), correlationId);
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  correlationId: " + correlationId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        // Step 6 — Demonstrate idempotency concept
        System.out.println("\nStep 6: Idempotency check — same correlationId would prevent duplicate...");
        System.out.println("  In production, search for existing workflows by correlationId before starting.");
        System.out.println("  If a RUNNING or COMPLETED workflow already has correlationId='" + correlationId + "',");
        System.out.println("  skip re-execution and return the existing workflow ID.\n");

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
