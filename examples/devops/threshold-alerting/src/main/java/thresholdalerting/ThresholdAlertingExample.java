package thresholdalerting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import thresholdalerting.workers.*;

import java.util.*;

/**
 * Example 423: Threshold Alerting
 *
 * Threshold-based alerting: check a metric value, SWITCH based on
 * severity (critical/warning/ok), and take the appropriate action.
 */
public class ThresholdAlertingExample {

    private static final List<String> TASK_NAMES = List.of(
            "th_check_metric", "th_page_oncall", "th_send_warning", "th_log_ok"
    );

    private static List<Worker> allWorkers() {
        return List.of(new CheckMetric(), new PageOncall(), new SendWarning(), new LogOk());
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 423: Threshold Alerting ===\n");

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
        input.put("metricName", "error_rate_percent");
        input.put("currentValue", 12.5);
        input.put("warningThreshold", 5);
        input.put("criticalThreshold", 10);

        String workflowId = client.startWorkflow("threshold_alerting_423", 1, input);
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        Map<String, Object> output = workflow.getOutput();

        System.out.println("  Severity: " + output.get("severity"));
        System.out.println("  Metric: " + output.get("metricName"));
        System.out.println("  Current Value: " + output.get("currentValue"));

        client.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
