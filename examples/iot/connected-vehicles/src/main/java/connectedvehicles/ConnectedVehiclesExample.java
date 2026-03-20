package connectedvehicles;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import connectedvehicles.workers.TelemetryWorker;
import connectedvehicles.workers.DiagnosticsWorker;
import connectedvehicles.workers.GeolocationWorker;
import connectedvehicles.workers.StatusReportWorker;
import java.util.List;
import java.util.Map;
public class ConnectedVehiclesExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 545: Connected Vehicles ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("veh_telemetry", "veh_diagnostics", "veh_geolocation", "veh_status_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TelemetryWorker(), new DiagnosticsWorker(), new GeolocationWorker(), new StatusReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("connected_vehicles_demo", 1, Map.of("vehicleId", "VEH-8821", "vin", "1HGBH41JXMN109186"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
