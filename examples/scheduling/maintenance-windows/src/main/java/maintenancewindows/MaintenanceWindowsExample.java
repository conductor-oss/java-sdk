package maintenancewindows;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import maintenancewindows.workers.*;
import java.util.List;
import java.util.Map;

public class MaintenanceWindowsExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 408: Maintenance Windows ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mnw_check_window", "mnw_execute_maintenance", "mnw_defer_maintenance"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckWindowWorker(), new ExecuteMaintenanceWorker(), new DeferMaintenanceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("maintenance_windows_408", 1, Map.of("system", "production-db", "maintenanceType", "database-optimization", "currentTime", "2026-03-08T03:30:00Z"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
