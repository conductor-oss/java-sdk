package multitenantapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import multitenantapproval.workers.MtaFinalizeWorker;
import multitenantapproval.workers.MtaLoadConfigWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Tenant Approval -- tenant-specific approval rules
 *
 * Demonstrates a workflow where different tenants have different approval
 * requirements based on their configuration and the request amount.
 *
 * Flow: load_config -> SWITCH(approvalLevel):
 *   "none"       -> skip (no wait)
 *   "manager"    -> 1 WAIT task (manager approval)
 *   "executive"  -> 2 WAIT tasks (manager + executive approval)
 * -> finalize
 *
 * Run:
 *   java -jar target/multi-tenant-approval-1.0.0.jar
 *   java -jar target/multi-tenant-approval-1.0.0.jar --workers
 */
public class MultiTenantApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Tenant Approval Demo: Tenant-Specific Approval Rules ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef loadConfigTask = new TaskDef();
        loadConfigTask.setName("mta_load_config");
        loadConfigTask.setTimeoutSeconds(60);
        loadConfigTask.setResponseTimeoutSeconds(30);
        loadConfigTask.setOwnerEmail("examples@orkes.io");

        TaskDef finalizeTask = new TaskDef();
        finalizeTask.setName("mta_finalize");
        finalizeTask.setTimeoutSeconds(60);
        finalizeTask.setResponseTimeoutSeconds(30);
        finalizeTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(loadConfigTask, finalizeTask));

        System.out.println("  Registered: mta_load_config, mta_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'multi_tenant_approval_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new MtaLoadConfigWorker(), new MtaFinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflow for small-biz (auto-approve, no wait)
        System.out.println("Step 4: Starting workflow for small-biz, amount=5000 (auto-approve)...\n");
        String workflowId = client.startWorkflow("multi_tenant_approval_demo", 1,
                Map.of("tenantId", "small-biz", "amount", 5000));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
