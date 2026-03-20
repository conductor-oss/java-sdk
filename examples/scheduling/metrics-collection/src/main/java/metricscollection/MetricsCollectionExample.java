package metricscollection;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import metricscollection.workers.*;
import java.util.List;
import java.util.Map;

public class MetricsCollectionExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 411: Metrics Collection ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mc_collect_app","mc_collect_infra","mc_collect_business","mc_aggregate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectAppWorker(), new CollectInfraWorker(), new CollectBusinessWorker(), new AggregateWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("metrics_collection_411", 1, Map.of("environment","production","timeRange","last-1h"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
