package healthcheckaggregation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import healthcheckaggregation.workers.*;
import java.util.List;
import java.util.Map;

public class HealthCheckAggregationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 317: Health Check Aggregation ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("hc_check_api", "hc_check_db", "hc_check_cache", "hc_check_queue", "hc_aggregate_health"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CheckApiWorker(), new CheckDbWorker(), new CheckCacheWorker(), new CheckQueueWorker(), new AggregateHealthWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("health_check_aggregation", 1, Map.of());
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Health: " + wf.getOutput().get("overallHealth"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
