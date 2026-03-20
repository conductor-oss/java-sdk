package loyaltyrewards;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import loyaltyrewards.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 737: Loyalty Rewards — Earn Points, Check Tier, Redeem, Track
 */
public class LoyaltyRewardsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 737: Loyalty Rewards ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("lyr_earn_points", "lyr_check_tier", "lyr_redeem", "lyr_track"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new EarnPointsWorker(), new CheckTierWorker(), new RedeemWorker(), new TrackWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("loyalty_rewards_737", 1,
                Map.of("customerId", "CUST-42", "orderTotal", 65, "redeemPoints", 200));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
