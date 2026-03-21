package costoptimization;

import com.netflix.conductor.client.worker.Worker;
import costoptimization.workers.CollectBillingWorker;
import costoptimization.workers.AnalyzeUsageWorker;
import costoptimization.workers.RecommendWorker;
import costoptimization.workers.ApplySavingsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 349: Cost Optimization — Cloud Spending Analysis and Reduction
 *
 * Pattern: collectbilling -> analyzeusage -> recommend -> applysavings
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 349: Cost Optimization ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "co_collect_billing",
                "co_analyze_usage",
                "co_recommend",
                "co_apply_savings"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectBillingWorker(),
                new AnalyzeUsageWorker(),
                new RecommendWorker(),
                new ApplySavingsWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("cost_optimization_workflow", 1, Map.of(
                "account", "prod-account-001",
                "period", "last-30-days"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_billingResult: " + execution.getOutput().get("collect_billingResult"));
        System.out.println("  apply_savingsResult: " + execution.getOutput().get("apply_savingsResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
