package crmagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import crmagent.workers.LookupCustomerWorker;
import crmagent.workers.CheckHistoryWorker;
import crmagent.workers.UpdateRecordWorker;
import crmagent.workers.GenerateResponseWorker;

import java.util.List;
import java.util.Map;

/**
 * CRM Agent Demo
 *
 * Demonstrates a sequential pipeline of four workers that handle a CRM
 * interaction: lookup customer, check history, update record, and generate response.
 *   cm_lookup_customer -> cm_check_history -> cm_update_record -> cm_generate_response
 *
 * Run:
 *   java -jar target/crm-agent-1.0.0.jar
 */
public class CrmAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== CRM Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cm_lookup_customer", "cm_check_history",
                "cm_update_record", "cm_generate_response"));
        System.out.println("  Registered: cm_lookup_customer, cm_check_history, cm_update_record, cm_generate_response\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'crm_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LookupCustomerWorker(),
                new CheckHistoryWorker(),
                new UpdateRecordWorker(),
                new GenerateResponseWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("crm_agent", 1,
                Map.of("customerId", "CUST-4821",
                        "inquiry", "Our API integration has been experiencing intermittent timeout errors since the last platform update. Can you investigate and provide a resolution timeline?",
                        "channel", "email"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
