package emailapproval;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import emailapproval.workers.PrepareWorker;
import emailapproval.workers.ProcessDecisionWorker;
import emailapproval.workers.SendEmailWorker;

import java.util.List;
import java.util.Map;

/**
 * Email-Based Approval with Click Links
 *
 * Demonstrates a human-in-the-loop workflow where:
 * 1. ea_prepare — prepares the approval request
 * 2. ea_send_email — sends an email with approve/reject links
 * 3. WAIT (email_response) — pauses until a user clicks a link
 * 4. ea_process_decision — processes the approval decision
 *
 * Run:
 *   java -jar target/email-approval-1.0.0.jar
 *   java -jar target/email-approval-1.0.0.jar --workers
 */
public class EmailApprovalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Email-Based Approval with Click Links Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareDef = new TaskDef();
        prepareDef.setName("ea_prepare");
        prepareDef.setTimeoutSeconds(60);
        prepareDef.setResponseTimeoutSeconds(30);
        prepareDef.setOwnerEmail("examples@orkes.io");

        TaskDef sendEmailDef = new TaskDef();
        sendEmailDef.setName("ea_send_email");
        sendEmailDef.setTimeoutSeconds(60);
        sendEmailDef.setResponseTimeoutSeconds(30);
        sendEmailDef.setOwnerEmail("examples@orkes.io");

        TaskDef processDecisionDef = new TaskDef();
        processDecisionDef.setName("ea_process_decision");
        processDecisionDef.setTimeoutSeconds(60);
        processDecisionDef.setResponseTimeoutSeconds(30);
        processDecisionDef.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareDef, sendEmailDef, processDecisionDef));

        System.out.println("  Registered: ea_prepare, ea_send_email, ea_process_decision\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'email_approval_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareWorker(),
                new SendEmailWorker(),
                new ProcessDecisionWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting email approval workflow...\n");
        String workflowId = client.startWorkflow("email_approval_workflow", 1,
                Map.of("requester", "user@example.com", "subject", "Expense Report #1234"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion (workflow will pause at WAIT task)...");
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
