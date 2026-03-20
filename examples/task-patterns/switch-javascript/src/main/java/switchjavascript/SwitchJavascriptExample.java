package switchjavascript;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import switchjavascript.workers.*;

import java.util.List;
import java.util.Map;

/**
 * SWITCH with JavaScript Evaluator — Complex Routing
 *
 * Demonstrates the SWITCH task with evaluatorType "javascript" for complex
 * multi-condition routing based on amount, customerType, and region.
 *
 * Routes:
 *   - VIP + amount > 1000 -> vip_high (VIP concierge)
 *   - VIP                 -> vip_standard
 *   - amount > 5000       -> needs_review (manual review)
 *   - region = EU         -> eu_processing (EU compliance)
 *   - default             -> standard
 *
 * After the SWITCH, swjs_finalize runs for all routes.
 *
 * Run:
 *   java -jar target/switch-javascript-1.0.0.jar
 *   java -jar target/switch-javascript-1.0.0.jar --workers
 */
public class SwitchJavascriptExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SWITCH with JavaScript Evaluator: Complex Routing ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "swjs_vip_concierge", "swjs_vip_standard", "swjs_manual_review",
                "swjs_eu_handler", "swjs_standard", "swjs_finalize"
        ));
        System.out.println("  Registered 6 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'switch_js_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VipConciergeWorker(),
                new VipStandardWorker(),
                new ManualReviewWorker(),
                new EuHandlerWorker(),
                new StandardWorker(),
                new FinalizeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in --workers mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: VIP + high amount -> vip_high
        System.out.println("--- Scenario 1: VIP customer, amount=$2000 ---");
        String wfId1 = client.startWorkflow("switch_js_demo", 1,
                Map.of("amount", 2000, "customerType", "vip", "region", "US"));
        System.out.println("  Workflow ID: " + wfId1);
        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Route:  " + wf1.getOutput().get("route"));
        System.out.println("  Amount: " + wf1.getOutput().get("amount"));
        if (!"COMPLETED".equals(wf1.getStatus().name()) || !"vip_high".equals(wf1.getOutput().get("route"))) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        // Scenario 2: VIP + low amount -> vip_standard
        System.out.println("--- Scenario 2: VIP customer, amount=$500 ---");
        String wfId2 = client.startWorkflow("switch_js_demo", 1,
                Map.of("amount", 500, "customerType", "vip", "region", "US"));
        System.out.println("  Workflow ID: " + wfId2);
        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        System.out.println("  Status: " + wf2.getStatus().name());
        System.out.println("  Route:  " + wf2.getOutput().get("route"));
        if (!"COMPLETED".equals(wf2.getStatus().name()) || !"vip_standard".equals(wf2.getOutput().get("route"))) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        // Scenario 3: Non-VIP, high amount -> needs_review
        System.out.println("--- Scenario 3: Regular customer, amount=$7500 ---");
        String wfId3 = client.startWorkflow("switch_js_demo", 1,
                Map.of("amount", 7500, "customerType", "regular", "region", "US"));
        System.out.println("  Workflow ID: " + wfId3);
        Workflow wf3 = client.waitForWorkflow(wfId3, "COMPLETED", 30000);
        System.out.println("  Status: " + wf3.getStatus().name());
        System.out.println("  Route:  " + wf3.getOutput().get("route"));
        if (!"COMPLETED".equals(wf3.getStatus().name()) || !"needs_review".equals(wf3.getOutput().get("route"))) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        // Scenario 4: EU region -> eu_processing
        System.out.println("--- Scenario 4: EU region, amount=$200 ---");
        String wfId4 = client.startWorkflow("switch_js_demo", 1,
                Map.of("amount", 200, "customerType", "regular", "region", "EU"));
        System.out.println("  Workflow ID: " + wfId4);
        Workflow wf4 = client.waitForWorkflow(wfId4, "COMPLETED", 30000);
        System.out.println("  Status: " + wf4.getStatus().name());
        System.out.println("  Route:  " + wf4.getOutput().get("route"));
        if (!"COMPLETED".equals(wf4.getStatus().name()) || !"eu_processing".equals(wf4.getOutput().get("route"))) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        // Scenario 5: Default -> standard
        System.out.println("--- Scenario 5: Default route, amount=$100 ---");
        String wfId5 = client.startWorkflow("switch_js_demo", 1,
                Map.of("amount", 100, "customerType", "regular", "region", "US"));
        System.out.println("  Workflow ID: " + wfId5);
        Workflow wf5 = client.waitForWorkflow(wfId5, "COMPLETED", 30000);
        System.out.println("  Status: " + wf5.getStatus().name());
        System.out.println("  Route:  " + wf5.getOutput().get("route"));
        if (!"COMPLETED".equals(wf5.getStatus().name()) || !"standard".equals(wf5.getOutput().get("route"))) {
            System.out.println("  UNEXPECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected.\n");
        }

        client.stopWorkers();

        if (allPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
