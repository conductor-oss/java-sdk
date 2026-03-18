package endtoendapp;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import endtoendapp.workers.AssignTicketWorker;
import endtoendapp.workers.ClassifyTicketWorker;
import endtoendapp.workers.NotifyCustomerWorker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Support Ticket Pipeline — A Complete End-to-End Conductor Application
 *
 * Demonstrates a realistic 3-step workflow:
 *   classify_ticket → assign_ticket → notify_customer
 *
 * Processes 3 tickets with different priorities and categories,
 * then prints a summary table.
 *
 * Run:
 *   java -jar target/end-to-end-app-1.0.0.jar
 *   java -jar target/end-to-end-app-1.0.0.jar --workers
 */
public class SupportTicketExample {

    private static final String WORKFLOW_NAME = "support_ticket_pipeline";
    private static final int WORKFLOW_VERSION = 1;

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Support Ticket Pipeline: End-to-End Application ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("classify_ticket", "assign_ticket", "notify_customer"));
        System.out.println("  Registered: classify_ticket, assign_ticket, notify_customer\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow '" + WORKFLOW_NAME + "'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyTicketWorker(),
                new AssignTicketWorker(),
                new NotifyCustomerWorker()
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

        // Step 4 — Submit 3 tickets
        System.out.println("Step 4: Submitting 3 support tickets...\n");

        List<Map<String, Object>> tickets = List.of(
                Map.of(
                        "ticketId", "TKT-001",
                        "subject", "Production database is down!",
                        "description", "The production database server is completely unresponsive. All services are affected.",
                        "category", "technical",
                        "customerEmail", "alice@example.com"
                ),
                Map.of(
                        "ticketId", "TKT-002",
                        "subject", "Question about billing",
                        "description", "I have a question about my last invoice. Can someone explain the charges?",
                        "category", "billing",
                        "customerEmail", "bob@example.com"
                ),
                Map.of(
                        "ticketId", "TKT-003",
                        "subject", "Login error after password reset",
                        "description", "I reset my password but now I get an error when trying to log in.",
                        "category", "account",
                        "customerEmail", "carol@example.com"
                )
        );

        List<String> workflowIds = new ArrayList<>();
        for (Map<String, Object> ticket : tickets) {
            String workflowId = client.startWorkflow(WORKFLOW_NAME, WORKFLOW_VERSION, ticket);
            workflowIds.add(workflowId);
            System.out.println("  Submitted " + ticket.get("ticketId") + " — Workflow ID: " + workflowId);
        }
        System.out.println();

        // Step 5 — Wait for all workflows to complete
        System.out.println("Step 5: Waiting for all tickets to be processed...\n");

        List<Workflow> completedWorkflows = new ArrayList<>();
        boolean allPassed = true;
        for (String workflowId : workflowIds) {
            Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
            completedWorkflows.add(workflow);
            String status = workflow.getStatus().name();
            if (!"COMPLETED".equals(status)) {
                allPassed = false;
            }
        }

        // Step 6 — Print summary table
        System.out.println("=== Ticket Processing Summary ===\n");
        System.out.printf("  %-10s | %-10s | %-22s | %-15s%n", "Ticket", "Priority", "Team", "Response Time");
        System.out.println("  " + "-".repeat(10) + " | " + "-".repeat(10) + " | " + "-".repeat(22) + " | " + "-".repeat(15));

        for (Workflow workflow : completedWorkflows) {
            Map<String, Object> output = workflow.getOutput();
            System.out.printf("  %-10s | %-10s | %-22s | %-15s%n",
                    output.get("ticketId"),
                    output.get("priority"),
                    output.get("team"),
                    output.get("estimatedResponse"));
        }
        System.out.println();

        client.stopWorkers();

        if (allPassed) {
            System.out.println("Result: PASSED — All 3 tickets processed successfully.");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED — Some tickets did not complete.");
            System.exit(1);
        }
    }
}
