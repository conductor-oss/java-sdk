package metricscollection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import metricscollection.workers.CollectAppMetrics;
import metricscollection.workers.CollectInfraMetrics;
import metricscollection.workers.CollectBusinessMetrics;
import metricscollection.workers.AggregateMetrics;

import java.util.List;
import java.util.Map;

/**
 * Example 411: Metrics Collection
 *
 * Collects metrics from multiple sources in parallel using FORK/JOIN,
 * then aggregates the results.
 *
 * Pattern:
 *   FORK(collect_app, collect_infra, collect_business) -> JOIN -> aggregate
 *
 * Run:
 *   java -jar target/metrics-collection-1.0.0.jar
 */
public class MetricsCollectionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 411: Metrics Collection ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mc_collect_app", "mc_collect_infra", "mc_collect_business", "mc_aggregate"));
        System.out.println("  Registered: mc_collect_app, mc_collect_infra, mc_collect_business, mc_aggregate\n");

        System.out.println("Step 2: Registering workflow 'metrics_collection_411'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectAppMetrics(),
                new CollectInfraMetrics(),
                new CollectBusinessMetrics(),
                new AggregateMetrics()
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
        String workflowId = client.startWorkflow("metrics_collection_411", 1,
                Map.of("environment", "production",
                       "timeRange", "last-1h"));
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
