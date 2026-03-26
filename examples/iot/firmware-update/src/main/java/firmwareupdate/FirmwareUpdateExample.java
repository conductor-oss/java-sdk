package firmwareupdate;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import firmwareupdate.workers.CheckVersionWorker;
import firmwareupdate.workers.DownloadWorker;
import firmwareupdate.workers.ValidateWorker;
import firmwareupdate.workers.DeployWorker;
import firmwareupdate.workers.VerifyWorker;
import java.util.List;
import java.util.Map;
public class FirmwareUpdateExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 533: Firmware Update ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("fw_check_version", "fw_download", "fw_validate", "fw_deploy", "fw_verify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CheckVersionWorker(), new DownloadWorker(), new ValidateWorker(), new DeployWorker(), new VerifyWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("firmware_update_workflow", 1, Map.of("deviceId", "DEV-533-GW-001", "currentVersion", "2.4.1", "targetVersion", "2.5.0"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
