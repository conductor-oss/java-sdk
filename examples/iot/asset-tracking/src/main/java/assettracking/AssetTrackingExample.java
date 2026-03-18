package assettracking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import assettracking.workers.TagAssetWorker;
import assettracking.workers.TrackLocationWorker;
import assettracking.workers.GeofenceCheckWorker;
import assettracking.workers.TriggerAlertWorker;
import assettracking.workers.UpdateRegistryWorker;
import java.util.List;
import java.util.Map;
public class AssetTrackingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 540: Asset Tracking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ast_tag_asset", "ast_track_location", "ast_geofence_check", "ast_trigger_alert", "ast_update_registry"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TagAssetWorker(), new TrackLocationWorker(), new GeofenceCheckWorker(), new TriggerAlertWorker(), new UpdateRegistryWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("asset_tracking_workflow", 1, Map.of("assetId", "ASSET-540-PALLET-001", "assetType", "shipping_pallet", "geofenceId", "GF-WAREHOUSE-A"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
