package energymanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import energymanagement.workers.MonitorConsumptionWorker;
import energymanagement.workers.AnalyzePatternsWorker;
import energymanagement.workers.OptimizeWorker;
import energymanagement.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Energy Management Demo
 *
 * Monitors consumption, analyzes patterns, optimizes, and reports.
 *
 * Run:
 *   java -jar target/energy-management-1.0.0.jar
 */
public class EnergyManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Energy Management Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "erg_monitor_consumption", "erg_analyze_patterns",
                "erg_optimize", "erg_report"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new MonitorConsumptionWorker(),
                new AnalyzePatternsWorker(),
                new OptimizeWorker(),
                new ReportWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String wfId = client.startWorkflow("energy_management_demo", 1,
                Map.of("buildingId", "BLDG-A1", "period", "2024-01"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(wf.getStatus().name())) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
