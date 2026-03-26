package distributedtracing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import distributedtracing.workers.*;
import java.util.List;
import java.util.Map;

public class DistributedTracingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 316: Distributed Tracing ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dt_create_trace", "dt_service_span", "dt_db_span", "dt_export_trace"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CreateTraceWorker(), new ServiceSpanWorker(), new DbSpanWorker(), new ExportTraceWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("distributed_tracing_workflow", 1,
                Map.of("requestId", "REQ-800", "operation", "get-user-profile"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Trace: " + wf.getOutput().get("traceId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
