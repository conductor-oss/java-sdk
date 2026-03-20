package salvagerecovery;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import salvagerecovery.workers.AssessDamageWorker;
import salvagerecovery.workers.SalvageWorker;
import salvagerecovery.workers.AuctionWorker;
import salvagerecovery.workers.SettleWorker;
import salvagerecovery.workers.CloseWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 701: Salvage Recovery
 */
public class SalvageRecoveryExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 701: Salvage Recovery ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("slv_assess_damage", "slv_salvage", "slv_auction", "slv_settle", "slv_close"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AssessDamageWorker(), new SalvageWorker(), new AuctionWorker(), new SettleWorker(), new CloseWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("slv_salvage_recovery", 1, Map.of("claimId", "CLM-701", "vehicleId", "VIN-98321", "damageType", "collision-total-loss"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("recoveryAmount: %s%n", workflow.getOutput().get("recoveryAmount"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
