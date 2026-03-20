package dashboarddata;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dashboarddata.workers.AggregateMetricsWorker;
import dashboarddata.workers.ComputeKpisWorker;
import dashboarddata.workers.BuildWidgetsWorker;
import dashboarddata.workers.CacheDashboardWorker;

import java.util.List;
import java.util.Map;

/**
 * Dashboard Data Workflow Demo
 *
 * Demonstrates a dashboard data preparation pipeline:
 *   dh_aggregate_metrics -> dh_compute_kpis -> dh_build_widgets -> dh_cache_dashboard
 *
 * Run:
 *   java -jar target/dashboard-data-1.0.0.jar
 */
public class DashboardDataExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Dashboard Data Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dh_aggregate_metrics", "dh_compute_kpis",
                "dh_build_widgets", "dh_cache_dashboard"));
        System.out.println("  Registered: dh_aggregate_metrics, dh_compute_kpis, dh_build_widgets, dh_cache_dashboard\n");

        System.out.println("Step 2: Registering workflow 'dashboard_data'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AggregateMetricsWorker(),
                new ComputeKpisWorker(),
                new BuildWidgetsWorker(),
                new CacheDashboardWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("dashboard_data", 1,
                Map.of("dashboardId", "exec-dashboard-001",
                        "timeRange", Map.of("start", "2024-03-15T00:00:00Z", "end", "2024-03-15T23:59:59Z"),
                        "refreshInterval", 300));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
