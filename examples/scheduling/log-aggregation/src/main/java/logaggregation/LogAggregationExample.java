package logaggregation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import logaggregation.workers.*;
import java.util.List;
import java.util.Map;

public class LogAggregationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 412: Log Aggregation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("la_collect_logs","la_parse_logs","la_enrich_logs","la_store_logs"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectLogsWorker(), new ParseLogsWorker(), new EnrichLogsWorker(), new StoreLogsWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("log_aggregation_412", 1, Map.of("sources", List.of("api-gateway","auth-service","payment-service"), "timeRange","last-1h","logLevel","INFO"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
