package distributedlogging;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import distributedlogging.workers.*;
import java.util.List;
import java.util.Map;

public class DistributedLoggingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 415: Distributed Logging ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dg_collect_svc1","dg_collect_svc2","dg_collect_svc3","dg_correlate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectSvc1Worker(), new CollectSvc2Worker(), new CollectSvc3Worker(), new CorrelateWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("distributed_logging_415", 1, Map.of("traceId","trace-abc123def456","timeRange","last-5m"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
