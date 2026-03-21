package custommetrics;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import custommetrics.workers.*;

import java.util.*;

/**
 * Example 422: Custom Metrics
 *
 * Custom metrics pipeline: define custom metrics, collect data points,
 * aggregate them, and update the dashboard.
 *
 * Pattern:
 *   define -> collect -> aggregate -> dashboard
 */
public class CustomMetricsExample {

    private static final List<String> TASK_NAMES = List.of(
            "cus_define_metrics",
            "cus_collect_data",
            "cus_aggregate",
            "cus_update_dashboard"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new DefineMetrics(),
                new CollectData(),
                new Aggregate(),
                new UpdateDashboard()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 422: Custom Metrics ===\n");

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
        input.put("metricDefinitions", List.of("order_processing_time", "cart_abandonment_rate", "checkout_success_count", "payment_retry_rate"));
        input.put("collectionInterval", "10s");
        input.put("aggregationWindow", "5m");

        String workflowId = client.startWorkflow("custom_metrics_422", 1, input);
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        Map<String, Object> output = workflow.getOutput();

        System.out.println("  Registered Metrics: " + output.get("registeredMetrics"));
        System.out.println("  Raw Data Points: " + output.get("rawDataPoints"));
        System.out.println("  Metric Count: " + output.get("metricCount"));
        System.out.println("  Dashboard Updated: " + output.get("dashboardUpdated"));

        client.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
