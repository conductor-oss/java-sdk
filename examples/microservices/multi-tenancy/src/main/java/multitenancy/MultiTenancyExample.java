package multitenancy;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multitenancy.workers.*;
import java.util.List;
import java.util.Map;

public class MultiTenancyExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 319: Multi-Tenancy ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mt_resolve_tenant", "mt_process_request", "mt_log_usage"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ResolveTenantWorker(), new ProcessRequestWorker(), new LogUsageWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("multi_tenancy_workflow", 1,
                Map.of("tenantId", "tenant-acme", "action", "generate-report", "data", Map.of("reportType", "monthly")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
