package capacityplanning;

import com.netflix.conductor.client.worker.Worker;
import capacityplanning.workers.CollectMetricsWorker;
import capacityplanning.workers.AnalyzeTrendsWorker;
import capacityplanning.workers.ForecastWorker;
import capacityplanning.workers.RecommendWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 340: Capacity Planning — Automated Resource Scaling Decisions
 *
 * Pattern: collectmetrics -> analyzetrends -> forecast -> recommend
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 340: Capacity Planning ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "cp_collect_metrics",
                "cp_analyze_trends",
                "cp_forecast",
                "cp_recommend"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectMetricsWorker(),
                new AnalyzeTrendsWorker(),
                new ForecastWorker(),
                new RecommendWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("capacity_planning_workflow", 1, Map.of(
                "service", "api-gateway",
                "period", "30d"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  recommendation: " + execution.getOutput().get("recommendation"));
        System.out.println("  daysLeft: " + execution.getOutput().get("daysLeft"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
