package incidentai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import incidentai.workers.*;
import java.util.List;
import java.util.Map;
public class IncidentAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 650: Incident AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("iai_detect", "iai_diagnose", "iai_suggest_fix", "iai_execute_fix", "iai_verify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetectWorker(), new DiagnoseWorker(), new SuggestFixWorker(), new ExecuteFixWorker(), new VerifyWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("iai_incident_ai", 1,
                Map.of("alertId", "ALT-9921", "serviceName", "checkout-service", "severity", "critical"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
