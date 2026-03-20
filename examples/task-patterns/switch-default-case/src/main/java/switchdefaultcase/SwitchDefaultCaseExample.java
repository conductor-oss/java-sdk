package switchdefaultcase;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import switchdefaultcase.workers.ProcessCardWorker;
import switchdefaultcase.workers.ProcessBankWorker;
import switchdefaultcase.workers.ProcessCryptoWorker;
import switchdefaultcase.workers.UnknownMethodWorker;
import switchdefaultcase.workers.LogWorker;

import java.util.List;
import java.util.Map;

/**
 * SWITCH defaultCase -- Fallback Routing for Unmatched Values
 *
 * Demonstrates the SWITCH task with a defaultCase that catches any payment
 * method not explicitly handled. Known methods route to specific processors;
 * unrecognized methods fall through to manual review.
 *
 * Cases:
 * - credit_card  -> dc_process_card   (Stripe)
 * - bank_transfer-> dc_process_bank   (Plaid)
 * - crypto       -> dc_process_crypto  (Coinbase)
 * - default      -> dc_unknown_method  (manual review)
 *
 * After switch: dc_log
 *
 * Run:
 *   java -jar target/switch-default-case-1.0.0.jar
 */
public class SwitchDefaultCaseExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SWITCH defaultCase: Fallback Routing for Unmatched Payment Methods ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dc_process_card", "dc_process_bank", "dc_process_crypto",
                "dc_unknown_method", "dc_log"));
        System.out.println("  Registered: dc_process_card, dc_process_bank, dc_process_crypto, dc_unknown_method, dc_log\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'default_case_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessCardWorker(),
                new ProcessBankWorker(),
                new ProcessCryptoWorker(),
                new UnknownMethodWorker(),
                new LogWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: credit_card -- processed by Stripe
        System.out.println("--- Scenario 1: credit_card payment ---");
        String wfId1 = client.startWorkflow("default_case_demo", 1,
                Map.of("paymentMethod", "credit_card"));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + wf1.getOutput());

        if (!"COMPLETED".equals(status1) || !Boolean.TRUE.equals(wf1.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: credit_card routed to Stripe and logged.\n");
        }

        // Scenario 2: bank_transfer -- processed by Plaid
        System.out.println("--- Scenario 2: bank_transfer payment ---");
        String wfId2 = client.startWorkflow("default_case_demo", 1,
                Map.of("paymentMethod", "bank_transfer"));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + wf2.getOutput());

        if (!"COMPLETED".equals(status2) || !Boolean.TRUE.equals(wf2.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: bank_transfer routed to Plaid and logged.\n");
        }

        // Scenario 3: crypto -- processed by Coinbase
        System.out.println("--- Scenario 3: crypto payment ---");
        String wfId3 = client.startWorkflow("default_case_demo", 1,
                Map.of("paymentMethod", "crypto"));
        System.out.println("  Workflow ID: " + wfId3);

        Workflow wf3 = client.waitForWorkflow(wfId3, "COMPLETED", 30000);
        String status3 = wf3.getStatus().name();
        System.out.println("  Status: " + status3);
        System.out.println("  Output: " + wf3.getOutput());

        if (!"COMPLETED".equals(status3) || !Boolean.TRUE.equals(wf3.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: crypto routed to Coinbase and logged.\n");
        }

        // Scenario 4: paypal -- unrecognized, hits default case
        System.out.println("--- Scenario 4: paypal (unrecognized -- hits default) ---");
        String wfId4 = client.startWorkflow("default_case_demo", 1,
                Map.of("paymentMethod", "paypal"));
        System.out.println("  Workflow ID: " + wfId4);

        Workflow wf4 = client.waitForWorkflow(wfId4, "COMPLETED", 30000);
        String status4 = wf4.getStatus().name();
        System.out.println("  Status: " + status4);
        System.out.println("  Output: " + wf4.getOutput());

        if (!"COMPLETED".equals(status4) || !Boolean.TRUE.equals(wf4.getOutput().get("logged"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with logged=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: paypal flagged for manual review and logged.\n");
        }

        // Summary
        System.out.println("Key insight: SWITCH defaultCase catches any value not listed in decisionCases,");
        System.out.println("providing a safe fallback for unexpected input without failing the workflow.");

        client.stopWorkers();

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
