package observabilitypipeline;

import com.netflix.conductor.client.worker.Worker;
import observabilitypipeline.workers.CollectMetricsWorker;
import observabilitypipeline.workers.CorrelateTracesWorker;
import observabilitypipeline.workers.DetectAnomaliesWorker;
import observabilitypipeline.workers.AlertOrStoreWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 365: Observability Pipeline — Telemetry Collection and Processing
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 365: Observability Pipeline ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "op_collect_metrics",
                "op_correlate_traces",
                "op_detect_anomalies",
                "op_alert_or_store"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectMetricsWorker(),
                new CorrelateTracesWorker(),
                new DetectAnomaliesWorker(),
                new AlertOrStoreWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("observability_pipeline_workflow", 1, Map.of(
                "service", "checkout-service",
                "timeWindow", "5m"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_metricsResult: " + execution.getOutput().get("collect_metricsResult"));
        System.out.println("  alert_or_storeResult: " + execution.getOutput().get("alert_or_storeResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
