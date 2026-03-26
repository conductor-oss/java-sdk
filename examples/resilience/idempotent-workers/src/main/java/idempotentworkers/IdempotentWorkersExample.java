package idempotentworkers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import idempotentworkers.workers.ChargeWorker;
import idempotentworkers.workers.SendEmailWorker;

import java.util.List;
import java.util.Map;

/**
 * Idempotent Workers — Handle Duplicate Execution Safely
 *
 * Demonstrates how to build workers that produce the same result when
 * called multiple times with the same input, preventing duplicate charges
 * and duplicate email notifications.
 *
 * Run:
 *   java -jar target/idempotent-workers-1.0.0.jar
 *   java -jar target/idempotent-workers-1.0.0.jar --workers
 */
public class IdempotentWorkersExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Idempotent Workers Demo: Handle Duplicate Execution Safely ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef chargeDef = new TaskDef();
        chargeDef.setName("idem_charge");
        chargeDef.setRetryCount(3);
        chargeDef.setRetryLogic(TaskDef.RetryLogic.FIXED);
        chargeDef.setRetryDelaySeconds(1);
        chargeDef.setTimeoutSeconds(60);
        chargeDef.setResponseTimeoutSeconds(30);
        chargeDef.setOwnerEmail("examples@orkes.io");

        TaskDef emailDef = new TaskDef();
        emailDef.setName("idem_send_email");
        emailDef.setRetryCount(3);
        emailDef.setRetryLogic(TaskDef.RetryLogic.FIXED);
        emailDef.setRetryDelaySeconds(1);
        emailDef.setTimeoutSeconds(60);
        emailDef.setResponseTimeoutSeconds(30);
        emailDef.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(chargeDef, emailDef));

        System.out.println("  Registered: idem_charge, idem_send_email");
        System.out.println("    Both with 3 retries (FIXED, 1s delay)\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'idempotent_workers_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ChargeWorker(), new SendEmailWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow for order ORD-001...\n");
        String workflowId = client.startWorkflow("idempotent_workers_demo", 1,
                Map.of("orderId", "ORD-001", "amount", 99.99, "email", "user@example.com"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
