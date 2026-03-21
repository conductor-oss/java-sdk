package serviceversioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import serviceversioning.workers.*;
import java.util.List;
import java.util.Map;

public class ServiceVersioningExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 320: Service Versioning ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sv_resolve_version", "sv_call_v1", "sv_call_v2", "sv_log_version_usage"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ResolveVersionWorker(), new CallV1Worker(), new CallV2Worker(), new LogVersionUsageWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("service_versioning_workflow", 1,
                Map.of("apiVersion", "v2", "request", Map.of("action", "list-items")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Version: " + wf.getOutput().get("version"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
