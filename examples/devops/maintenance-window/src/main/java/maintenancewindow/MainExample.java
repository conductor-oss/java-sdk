package maintenancewindow;

import com.netflix.conductor.client.worker.Worker;
import maintenancewindow.workers.NotifyStartWorker;
import maintenancewindow.workers.SuppressAlertsWorker;
import maintenancewindow.workers.ExecuteMaintenanceWorker;
import maintenancewindow.workers.RestoreNormalWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 336: Maintenance Window — Scheduled Maintenance Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 336: Maintenance Window ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "mw_notify_start",
                "mw_suppress_alerts",
                "mw_execute_maintenance",
                "mw_restore_normal"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new NotifyStartWorker(),
                new SuppressAlertsWorker(),
                new ExecuteMaintenanceWorker(),
                new RestoreNormalWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("maintenance_window_workflow", 1, Map.of(
                "system", "database-cluster",
                "maintenanceType", "version-upgrade",
                "duration", "2h"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  notify_startResult: " + execution.getOutput().get("notify_startResult"));
        System.out.println("  restore_normalResult: " + execution.getOutput().get("restore_normalResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
