package clinicaltrials;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import clinicaltrials.workers.*;
import java.util.List;
import java.util.Map;
public class ClinicalTrialsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 476: Clinical Trials ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("clt_screen", "clt_consent", "clt_randomize", "clt_monitor", "clt_analyze"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ScreenWorker(), new ConsentWorker(), new RandomizeWorker(), new MonitorWorker(), new AnalyzeTrialWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("clinical_trials_workflow", 1, Map.of("trialId", "TRIAL-2024-CARDIO-001", "participantId", "SUBJ-4401", "condition", "hypertension"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
