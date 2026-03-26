package iotsecurity;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import iotsecurity.workers.ScanDevicesWorker;
import iotsecurity.workers.DetectVulnerabilitiesWorker;
import iotsecurity.workers.PatchWorker;
import iotsecurity.workers.VerifyWorker;
import java.util.List;
import java.util.Map;
public class IotSecurityExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 550: IoT Security ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ios_scan_devices", "ios_detect_vulnerabilities", "ios_patch", "ios_verify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ScanDevicesWorker(), new DetectVulnerabilitiesWorker(), new PatchWorker(), new VerifyWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("iot_security_demo", 1, Map.of("networkId", "NET-OFFICE-1", "scanDepth", "full"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
