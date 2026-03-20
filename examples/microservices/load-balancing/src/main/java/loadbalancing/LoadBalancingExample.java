package loadbalancing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import loadbalancing.workers.*;

import java.util.List;
import java.util.Map;

public class LoadBalancingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Load Balancing Demo ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lb_call_instance", "lb_aggregate_results"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CallInstanceWorker(), new AggregateResultsWorker());
        client.startWorkers(workers);

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("load_balancing_294", 1,
                Map.of("requestBatch", Map.of("totalRecords", 150, "source", "api-ingest")));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("Status: " + workflow.getStatus().name());
        client.stopWorkers();
        System.exit("COMPLETED".equals(workflow.getStatus().name()) ? 0 : 1);
    }
}
