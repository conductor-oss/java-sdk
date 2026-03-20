package custommetrics;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import custommetrics.workers.*;
import java.util.List;
import java.util.Map;

public class CustomMetricsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 422: Custom Metrics ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cus_define_metrics", "cus_collect_data", "cus_aggregate", "cus_update_dashboard"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DefineMetricsWorker(), new CollectDataWorker(), new CusAggregateWorker(), new UpdateDashboardWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("custom_metrics_422", 1, Map.of("metricDefinitions",List.of("order_processing_time","cart_abandonment_rate"),"collectionInterval","10s","aggregationWindow","5m"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
