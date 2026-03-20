package hubspotintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import hubspotintegration.workers.CreateContactWorker;
import hubspotintegration.workers.EnrichDataWorker;
import hubspotintegration.workers.AssignOwnerWorker;
import hubspotintegration.workers.NurtureSequenceWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 439: HubSpot Integration
 *
 * Performs a HubSpot CRM integration workflow:
 * create contact -> enrich data -> assign owner -> nurture sequence.
 *
 * Run:
 *   java -jar target/hubspot-integration-1.0.0.jar
 */
public class HubspotIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 439: HubSpot Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "hs_create_contact", "hs_enrich_data", "hs_assign_owner", "hs_nurture_sequence"));
        System.out.println("  Registered: hs_create_contact, hs_enrich_data, hs_assign_owner, hs_nurture_sequence\n");

        System.out.println("Step 2: Registering workflow \'hubspot_integration_439\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateContactWorker(),
                new EnrichDataWorker(),
                new AssignOwnerWorker(),
                new NurtureSequenceWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("hubspot_integration_439", 1,
                Map.of("email", "bob.smith@techcorp.io",
                        "firstName", "Bob",
                        "lastName", "Smith",
                        "company", "TechCorp"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Contact ID: " + workflow.getOutput().get("contactId"));
        System.out.println("  Segment: " + workflow.getOutput().get("segment"));
        System.out.println("  Owner: " + workflow.getOutput().get("owner"));
        System.out.println("  Nurture Sequence: " + workflow.getOutput().get("nurtureSequence"));

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
