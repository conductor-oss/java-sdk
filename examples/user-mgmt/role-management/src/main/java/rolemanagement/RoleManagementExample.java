package rolemanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import rolemanagement.workers.*;
import java.util.List;
import java.util.Map;

public class RoleManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 613: Role Management ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rom_request_role", "rom_validate", "rom_assign", "rom_sync_permissions"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new RequestRoleWorker(), new ValidateRoleWorker(), new AssignRoleWorker(), new SyncPermissionsWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("rom_role_management", 1,
                Map.of("userId", "USR-ROM01", "requestedRole", "admin", "requestedBy", "MGR-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
