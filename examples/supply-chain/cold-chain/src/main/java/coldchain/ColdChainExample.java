package coldchain;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import coldchain.workers.*;
import java.util.*;
public class ColdChainExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 670: Cold Chain Monitoring ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cch_monitor_temp","cch_check_thresholds","cch_handle_ok","cch_handle_alert","cch_act"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new MonitorTempWorker(), new CheckThresholdsWorker(), new HandleOkWorker(), new HandleAlertWorker(), new ActWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cch_cold_chain", 1,
                Map.of("shipmentId","COLD-670-001","product","Frozen Pharmaceuticals","minTemp",2,"maxTemp",8));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
