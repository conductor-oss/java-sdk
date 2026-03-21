package predictivemonitoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import predictivemonitoring.workers.*;

import java.util.*;

/**
 * Example 424: Predictive Monitoring
 *
 * Predictive monitoring: collect historical data, train a prediction
 * model, predict future metrics, and alert if anomalies are forecast.
 *
 * Pattern:
 *   collect_history -> train_model -> predict -> alert
 */
public class PredictiveMonitoringExample {

    private static final List<String> TASK_NAMES = List.of(
            "pdm_collect_history", "pdm_train_model", "pdm_predict", "pdm_alert"
    );

    private static List<Worker> allWorkers() {
        return List.of(new CollectHistory(), new TrainModel(), new Predict(), new PdmAlert());
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 424: Predictive Monitoring ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(TASK_NAMES);
        client.registerWorkflow("workflow.json");

        List<Worker> workers = allWorkers();
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("metricName", "cpu_usage_percent");
        input.put("historyDays", 30);
        input.put("forecastHours", 12);

        String workflowId = client.startWorkflow("predictive_monitoring_424", 1, input);
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        Map<String, Object> output = workflow.getOutput();

        System.out.println("  Data Points: " + output.get("dataPoints"));
        System.out.println("  Model Accuracy: " + output.get("modelAccuracy") + "%");
        System.out.println("  Predicted Peak: " + output.get("predictedPeak") + "%");
        System.out.println("  Breach Likelihood: " + output.get("breachLikelihood") + "%");
        System.out.println("  Alert Sent: " + output.get("alertSent"));

        client.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
