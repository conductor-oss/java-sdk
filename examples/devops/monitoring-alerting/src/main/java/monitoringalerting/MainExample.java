package monitoringalerting;

import com.netflix.conductor.client.worker.Worker;
import monitoringalerting.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example monitoring-alerting: Monitoring and Alerting — Intelligent Alert Orchestration
 *
 * Pattern: evaluate -> deduplicate -> enrich -> route
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example monitoring-alerting: Monitoring and Alerting ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        // Register task definitions
        helper.registerTaskDefs(List.of(
                "ma_evaluate", "ma_deduplicate", "ma_enrich", "ma_route"
        ));

        // Register workflow
        helper.registerWorkflow("workflow.json");

        // Start workers
        List<Worker> workers = List.of(
                new EvaluateWorker(),
                new DeduplicateWorker(),
                new EnrichWorker(),
                new RouteWorker()
        );
        helper.startWorkers(workers);

        // Start workflow
        String workflowId = helper.startWorkflow("monitoring_alerting_workflow", 1, Map.of(
                "alertName", "high-cpu",
                "metric", "cpu",
                "value", 92
        ));

        // Wait for completion
        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  severity: " + execution.getOutput().get("severity"));
        System.out.println("  routed: " + execution.getOutput().get("routed"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
