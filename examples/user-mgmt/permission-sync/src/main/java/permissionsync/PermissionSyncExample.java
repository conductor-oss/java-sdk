package permissionsync;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import permissionsync.workers.*;
import java.util.List;
import java.util.Map;

public class PermissionSyncExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 614: Permission Sync ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pms_scan_systems", "pms_diff", "pms_sync", "pms_verify"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new ScanSystemsWorker(), new DiffWorker(), new SyncWorker(), new VerifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("pms_permission_sync", 1,
                Map.of("sourceSystem", "ldap", "targetSystems", List.of("api-gateway", "database", "dashboard")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
