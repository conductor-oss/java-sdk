package supplychainiot;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import supplychainiot.workers.TrackShipmentWorker;
import supplychainiot.workers.MonitorConditionsWorker;
import supplychainiot.workers.HandleOkWorker;
import supplychainiot.workers.HandleAlertWorker;
import java.util.List;
import java.util.Map;
public class SupplyChainIotExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 543: Supply Chain IoT ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sci_track_shipment", "sci_monitor_conditions", "sci_handle_ok", "sci_handle_alert"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TrackShipmentWorker(), new MonitorConditionsWorker(), new HandleOkWorker(), new HandleAlertWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("supply_chain_iot_demo", 1, Map.of("shipmentId", "SHIP-OK", "origin", "New York", "destination", "Los Angeles"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
