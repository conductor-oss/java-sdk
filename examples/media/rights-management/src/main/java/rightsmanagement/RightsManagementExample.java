package rightsmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import rightsmanagement.workers.CheckLicenseWorker;
import rightsmanagement.workers.VerifyUsageWorker;
import rightsmanagement.workers.TrackRoyaltiesWorker;
import rightsmanagement.workers.GenerateReportWorker;
import java.util.List;
import java.util.Map;
public class RightsManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 529: Rights Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rts_check_license", "rts_verify_usage", "rts_track_royalties", "rts_generate_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CheckLicenseWorker(), new VerifyUsageWorker(), new TrackRoyaltiesWorker(), new GenerateReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rights_management_workflow", 1, Map.of("assetId", "SONG-529-001", "licenseId", "LIC-MUS-2026-450", "usageType", "streaming", "territory", "US"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
