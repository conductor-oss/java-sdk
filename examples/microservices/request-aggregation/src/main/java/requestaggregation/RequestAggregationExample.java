package requestaggregation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import requestaggregation.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Request Aggregation Demo
 *
 * Aggregates data from user, orders, and recommendations services in parallel,
 * then merges results into a single response.
 */
public class RequestAggregationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Request Aggregation Demo ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("agg_fetch_user", "agg_fetch_orders", "agg_fetch_recommendations", "agg_merge_results"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new FetchUserWorker(), new FetchOrdersWorker(),
                new FetchRecommendationsWorker(), new MergeResultsWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("request_aggregation_workflow", 1, Map.of("userId", "user-42"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
