package multistepcompensation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import multistepcompensation.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Multi-Step Compensation — Reverse Operations in Order
 *
 * Demonstrates a saga-like pattern where a forward workflow performs a sequence
 * of steps (create account, setup billing, provision resources), and if any step
 * fails, a compensation workflow undoes the completed steps in reverse order.
 *
 * Run:
 *   java -jar target/multi-step-compensation-1.0.0.jar
 *   java -jar target/multi-step-compensation-1.0.0.jar --workers
 */
public class MultiStepCompensationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Step Compensation Demo: Reverse Operations in Order ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        List<TaskDef> taskDefs = List.of(
                createTaskDef("msc_create_account"),
                createTaskDef("msc_setup_billing"),
                createTaskDef("msc_provision_resources"),
                createTaskDef("msc_undo_provision"),
                createTaskDef("msc_undo_billing"),
                createTaskDef("msc_undo_account")
        );
        client.registerTaskDefs(taskDefs);
        System.out.println("  Registered 6 task definitions.\n");

        // Step 2 — Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("forward-workflow.json");
        client.registerWorkflow("compensation-workflow.json");
        System.out.println("  Forward and compensation workflows registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateAccountWorker(),
                new SetupBillingWorker(),
                new ProvisionResourcesWorker(),
                new UndoProvisionWorker(),
                new UndoBillingWorker(),
                new UndoAccountWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run forward workflow (success case)
        System.out.println("Step 4: Running forward workflow (no failure)...\n");
        String successId = client.startWorkflow("msc_forward_workflow", 1,
                Map.of("failAt", "none"));
        System.out.println("  Workflow ID: " + successId);
        Workflow successWf = client.waitForWorkflow(successId, "COMPLETED", 30000);
        System.out.println("  Status: " + successWf.getStatus().name());
        System.out.println("  Output: " + successWf.getOutput() + "\n");

        // Step 5 — Run forward workflow (failure at provision)
        System.out.println("Step 5: Running forward workflow (failAt=provision)...\n");
        String failId = client.startWorkflow("msc_forward_workflow", 1,
                Map.of("failAt", "provision"));
        System.out.println("  Workflow ID: " + failId);
        Workflow failWf = client.waitForWorkflow(failId, "FAILED", 30000);
        System.out.println("  Status: " + failWf.getStatus().name());

        // Step 6 — Run compensation workflow
        System.out.println("\nStep 6: Running compensation workflow (reverse order)...\n");
        String compensateId = client.startWorkflow("msc_compensation_workflow", 1,
                Map.of("accountId", "ACCT-001", "billingId", "BILL-001", "resourceId", "RES-001"));
        System.out.println("  Workflow ID: " + compensateId);
        Workflow compensateWf = client.waitForWorkflow(compensateId, "COMPLETED", 30000);
        System.out.println("  Status: " + compensateWf.getStatus().name());
        System.out.println("  Output: " + compensateWf.getOutput());

        client.stopWorkers();

        boolean passed = "COMPLETED".equals(successWf.getStatus().name())
                && "FAILED".equals(failWf.getStatus().name())
                && "COMPLETED".equals(compensateWf.getStatus().name());

        if (passed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }

    private static TaskDef createTaskDef(String name) {
        TaskDef def = new TaskDef();
        def.setName(name);
        def.setRetryCount(0);
        def.setTimeoutSeconds(60);
        def.setResponseTimeoutSeconds(30);
        def.setOwnerEmail("examples@orkes.io");
        return def;
    }
}
