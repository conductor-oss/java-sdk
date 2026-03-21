package changetracking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import changetracking.workers.*;
import java.util.List;
import java.util.Map;

public class ChangeTrackingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 427: Change Tracking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("chg_detect_change", "chg_diff", "chg_classify", "chg_record"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DetectChangeWorker(), new DiffWorker(), new ClassifyChangeWorker(), new RecordChangeWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("change_tracking_427", 1, Map.of("resourceType","service","resourceId","api-gateway","changeSource","ci-cd-pipeline"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
