package nestedswitch;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import nestedswitch.workers.NsUsPremiumWorker;
import nestedswitch.workers.NsUsStandardWorker;
import nestedswitch.workers.NsEuPremiumWorker;
import nestedswitch.workers.NsEuStandardWorker;
import nestedswitch.workers.NsOtherRegionWorker;
import nestedswitch.workers.NsCompleteWorker;

import java.util.List;
import java.util.Map;

/**
 * Nested SWITCH -- Multi-level Decision Trees
 *
 * Demonstrates nested SWITCH tasks with evaluatorType "value-param" for
 * multi-level routing. The outer switch routes by region (US, EU, default),
 * and inner switches route by tier (premium, default).
 *
 * Level 1 (region):
 * - US   -> Level 2 switch on tier
 * - EU   -> Level 2 switch on tier
 * - default -> ns_other_region
 *
 * Level 2 (tier inside US):
 * - premium -> ns_us_premium
 * - default -> ns_us_standard
 *
 * Level 2 (tier inside EU):
 * - premium -> ns_eu_premium
 * - default -> ns_eu_standard
 *
 * After all branches: ns_complete
 *
 * Run:
 *   java -jar target/nested-switch-1.0.0.jar
 */
public class NestedSwitchExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Nested SWITCH: Multi-level Decision Trees ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ns_us_premium", "ns_us_standard",
                "ns_eu_premium", "ns_eu_standard",
                "ns_other_region", "ns_complete"));
        System.out.println("  Registered: ns_us_premium, ns_us_standard, ns_eu_premium, ns_eu_standard, ns_other_region, ns_complete\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'nested_switch_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new NsUsPremiumWorker(),
                new NsUsStandardWorker(),
                new NsEuPremiumWorker(),
                new NsEuStandardWorker(),
                new NsOtherRegionWorker(),
                new NsCompleteWorker());
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: US + premium
        System.out.println("--- Scenario 1: US region, premium tier ---");
        String wfId1 = client.startWorkflow("nested_switch_demo", 1,
                Map.of("region", "US", "tier", "premium", "amount", 500));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + wf1.getOutput());

        if (!"COMPLETED".equals(status1) || !Boolean.TRUE.equals(wf1.getOutput().get("done"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with done=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: US premium request routed correctly.\n");
        }

        // Scenario 2: US + standard (default tier)
        System.out.println("--- Scenario 2: US region, standard tier ---");
        String wfId2 = client.startWorkflow("nested_switch_demo", 1,
                Map.of("region", "US", "tier", "standard", "amount", 100));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + wf2.getOutput());

        if (!"COMPLETED".equals(status2) || !Boolean.TRUE.equals(wf2.getOutput().get("done"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with done=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: US standard request routed correctly.\n");
        }

        // Scenario 3: EU + premium
        System.out.println("--- Scenario 3: EU region, premium tier ---");
        String wfId3 = client.startWorkflow("nested_switch_demo", 1,
                Map.of("region", "EU", "tier", "premium", "amount", 750));
        System.out.println("  Workflow ID: " + wfId3);

        Workflow wf3 = client.waitForWorkflow(wfId3, "COMPLETED", 30000);
        String status3 = wf3.getStatus().name();
        System.out.println("  Status: " + status3);
        System.out.println("  Output: " + wf3.getOutput());

        if (!"COMPLETED".equals(status3) || !Boolean.TRUE.equals(wf3.getOutput().get("done"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with done=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: EU premium request routed correctly.\n");
        }

        // Scenario 4: EU + standard (default tier)
        System.out.println("--- Scenario 4: EU region, standard tier ---");
        String wfId4 = client.startWorkflow("nested_switch_demo", 1,
                Map.of("region", "EU", "tier", "standard", "amount", 200));
        System.out.println("  Workflow ID: " + wfId4);

        Workflow wf4 = client.waitForWorkflow(wfId4, "COMPLETED", 30000);
        String status4 = wf4.getStatus().name();
        System.out.println("  Status: " + status4);
        System.out.println("  Output: " + wf4.getOutput());

        if (!"COMPLETED".equals(status4) || !Boolean.TRUE.equals(wf4.getOutput().get("done"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with done=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: EU standard request routed correctly.\n");
        }

        // Scenario 5: APAC (default region)
        System.out.println("--- Scenario 5: APAC region (default -- hits other) ---");
        String wfId5 = client.startWorkflow("nested_switch_demo", 1,
                Map.of("region", "APAC", "tier", "premium", "amount", 300));
        System.out.println("  Workflow ID: " + wfId5);

        Workflow wf5 = client.waitForWorkflow(wfId5, "COMPLETED", 30000);
        String status5 = wf5.getStatus().name();
        System.out.println("  Status: " + status5);
        System.out.println("  Output: " + wf5.getOutput());

        if (!"COMPLETED".equals(status5) || !Boolean.TRUE.equals(wf5.getOutput().get("done"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with done=true\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: APAC request routed to other region handler.\n");
        }

        // Summary
        System.out.println("Key insight: Nested SWITCH tasks create multi-level decision trees.");
        System.out.println("The outer SWITCH routes by region, and inner SWITCHes route by tier.");

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
