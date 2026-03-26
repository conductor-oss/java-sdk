package geofencing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import geofencing.workers.CheckLocationWorker;
import geofencing.workers.EvaluateBoundariesWorker;
import geofencing.workers.AlertInsideWorker;
import geofencing.workers.AlertOutsideWorker;

import java.util.List;
import java.util.Map;

/**
 * Geofencing Demo
 *
 * Demonstrates location boundary monitoring:
 *   geo_check_location -> geo_evaluate_boundaries -> SWITCH(zoneStatus:
 *       inside  -> geo_alert_inside,
 *       outside -> geo_alert_outside)
 *
 * Run:
 *   java -jar target/geofencing-1.0.0.jar
 */
public class GeofencingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Geofencing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "geo_check_location", "geo_evaluate_boundaries",
                "geo_alert_inside", "geo_alert_outside"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'geofencing_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckLocationWorker(),
                new EvaluateBoundariesWorker(),
                new AlertInsideWorker(),
                new AlertOutsideWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow (inside zone)...\n");
        String wfId1 = client.startWorkflow("geofencing_demo", 1,
                Map.of("deviceId", "DEV-101",
                        "latitude", 37.78,
                        "longitude", -122.42));
        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 60000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Output: " + wf1.getOutput() + "\n");

        System.out.println("Step 5: Starting workflow (outside zone)...\n");
        String wfId2 = client.startWorkflow("geofencing_demo", 1,
                Map.of("deviceId", "DEV-202",
                        "latitude", 37.80,
                        "longitude", -122.45));
        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 60000);
        System.out.println("  Status: " + wf2.getStatus().name());
        System.out.println("  Output: " + wf2.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(wf1.getStatus().name()) && "COMPLETED".equals(wf2.getStatus().name())) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
