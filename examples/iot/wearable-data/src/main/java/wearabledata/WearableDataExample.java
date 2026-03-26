package wearabledata;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import wearabledata.workers.CollectVitalsWorker;
import wearabledata.workers.ProcessDataWorker;
import wearabledata.workers.DetectAnomaliesWorker;
import wearabledata.workers.NotifyWorker;
import java.util.List;
import java.util.Map;
public class WearableDataExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 544: Wearable Data ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("wer_collect_vitals", "wer_process_data", "wer_detect_anomalies", "wer_notify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectVitalsWorker(), new ProcessDataWorker(), new DetectAnomaliesWorker(), new NotifyWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("wearable_data_demo", 1, Map.of("userId", "USER-42", "deviceId", "WATCH-XR7"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
