package loyaltyprogram;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import loyaltyprogram.workers.*;
import java.util.List;
import java.util.Map;

/** Example 466: Loyalty Program */
public class LoyaltyProgramExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 466: Loyalty Program ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("loy_earn_points", "loy_check_tier", "loy_upgrade_tier", "loy_reward"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new EarnPointsWorker(), new CheckTierWorker(), new UpgradeTierWorker(), new RewardWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("loyalty_program_workflow", 1, Map.of("customerId", "CUST-7712", "purchaseAmount", 150, "currentTier", "Silver"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
