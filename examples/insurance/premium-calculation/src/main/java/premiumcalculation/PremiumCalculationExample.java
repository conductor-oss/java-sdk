package premiumcalculation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import premiumcalculation.workers.CollectFactorsWorker;
import premiumcalculation.workers.CalculateBaseWorker;
import premiumcalculation.workers.ApplyModifiersWorker;
import premiumcalculation.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 708: Premium Calculation
 */
public class PremiumCalculationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 708: Premium Calculation ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("pmc_collect_factors", "pmc_calculate_base", "pmc_apply_modifiers", "pmc_finalize"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectFactorsWorker(), new CalculateBaseWorker(), new ApplyModifiersWorker(), new FinalizeWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("pmc_premium_calculation", 1, Map.of("policyType", "auto", "applicantAge", 35, "coverageAmount", 100000));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("finalPremium: %s%n", workflow.getOutput().get("finalPremium"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
