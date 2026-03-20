package zendeskintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import zendeskintegration.workers.CreateTicketWorker;
import zendeskintegration.workers.ClassifyTicketWorker;
import zendeskintegration.workers.RouteTicketWorker;
import zendeskintegration.workers.ResolveTicketWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 440: Zendesk Integration
 *
 * Performs a Zendesk support integration workflow:
 * create ticket -> classify priority -> route to agent -> resolve.
 *
 * Run:
 *   java -jar target/zendesk-integration-1.0.0.jar
 */
public class ZendeskIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 440: Zendesk Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "zd_create_ticket", "zd_classify", "zd_route", "zd_resolve"));
        System.out.println("  Registered: zd_create_ticket, zd_classify, zd_route, zd_resolve\n");

        System.out.println("Step 2: Registering workflow \'zendesk_integration_440\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateTicketWorker(),
                new ClassifyTicketWorker(),
                new RouteTicketWorker(),
                new ResolveTicketWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("zendesk_integration_440", 1,
                Map.of("requesterEmail", "user@company.com",
                        "subject", "Cannot access dashboard",
                        "description", "Getting 403 error when accessing the analytics dashboard",
                        "category", "access-issues"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Ticket ID: " + workflow.getOutput().get("ticketId"));
        System.out.println("  Priority: " + workflow.getOutput().get("priority"));
        System.out.println("  Agent: " + workflow.getOutput().get("agent"));
        System.out.println("  Resolved: " + workflow.getOutput().get("resolved"));

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
