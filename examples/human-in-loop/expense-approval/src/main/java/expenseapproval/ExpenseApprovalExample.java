package expenseapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import expenseapproval.workers.ProcessWorker;
import expenseapproval.workers.ValidatePolicyWorker;

import java.util.List;
import java.util.Map;

/**
 * Expense Approval Workflow -- Policy Validation with Conditional Approval
 *
 * Demonstrates a human-in-the-loop pattern where:
 * 1. validate_policy checks if the expense requires approval
 * 2. A SWITCH routes based on approvalRequired:
 *    - "true"  -> WAIT task (human approval gate)
 *    - default -> skip straight to processing
 * 3. process finalizes the expense
 *
 * Run:
 *   java -jar target/expense-approval-1.0.0.jar
 *   java -jar target/expense-approval-1.0.0.jar --workers
 */
public class ExpenseApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Expense Approval Demo: Policy Validation with Conditional Approval ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef validatePolicyTask = new TaskDef();
        validatePolicyTask.setName("exp_validate_policy");
        validatePolicyTask.setTimeoutSeconds(60);
        validatePolicyTask.setResponseTimeoutSeconds(30);
        validatePolicyTask.setOwnerEmail("examples@orkes.io");

        TaskDef processTask = new TaskDef();
        processTask.setName("exp_process");
        processTask.setTimeoutSeconds(60);
        processTask.setResponseTimeoutSeconds(30);
        processTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(validatePolicyTask, processTask));

        System.out.println("  Registered: exp_validate_policy, exp_process\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'expense_approval'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ValidatePolicyWorker(), new ProcessWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflow with a small expense (no approval needed)
        System.out.println("Step 4: Starting workflow (amount=50, category=office)...\n");
        String workflowId = client.startWorkflow("expense_approval", 1,
                Map.of("amount", 50, "category", "office"));
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
