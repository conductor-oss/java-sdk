package salesforceintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import salesforceintegration.workers.QueryLeadsWorker;
import salesforceintegration.workers.ScoreLeadsWorker;
import salesforceintegration.workers.UpdateRecordsWorker;
import salesforceintegration.workers.SyncCrmWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 438: Salesforce Integration
 *
 * Performs a Salesforce integration workflow:
 * query leads -> score leads -> update records -> sync to CRM.
 *
 * Run:
 *   java -jar target/salesforce-integration-1.0.0.jar
 */
public class SalesforceIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 438: Salesforce Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sfc_query_leads", "sfc_score_leads", "sfc_update_records", "sfc_sync_crm"));
        System.out.println("  Registered: sfc_query_leads, sfc_score_leads, sfc_update_records, sfc_sync_crm\n");

        System.out.println("Step 2: Registering workflow \'salesforce_integration_438\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new QueryLeadsWorker(),
                new ScoreLeadsWorker(),
                new UpdateRecordsWorker(),
                new SyncCrmWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("salesforce_integration_438", 1,
                Map.of("query", "industry = 'Technology' AND status = 'New'",
                        "scoringModel", "ml-lead-v2",
                        "syncTarget", "marketing-hub"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Leads Queried: " + workflow.getOutput().get("leadsQueried"));
        System.out.println("  Leads Scored: " + workflow.getOutput().get("leadsScored"));
        System.out.println("  Records Updated: " + workflow.getOutput().get("recordsUpdated"));
        System.out.println("  Synced: " + workflow.getOutput().get("synced"));

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
