package fleetmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import fleetmanagement.workers.TrackVehiclesWorker;
import fleetmanagement.workers.OptimizeRoutesWorker;
import fleetmanagement.workers.DispatchWorker;
import fleetmanagement.workers.MonitorTripWorker;
import fleetmanagement.workers.GenerateReportWorker;
import java.util.List;
import java.util.Map;
public class FleetManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 535: Fleet Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("flt_track_vehicles", "flt_optimize_routes", "flt_dispatch", "flt_monitor_trip", "flt_generate_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TrackVehiclesWorker(), new OptimizeRoutesWorker(), new DispatchWorker(), new MonitorTripWorker(), new GenerateReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("fleet_management_workflow", 1, Map.of("tripId", "TRIP-535-001", "origin", "San Francisco, CA", "destination", "San Jose, CA", "fleetId", "FLEET-BAY-AREA"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
