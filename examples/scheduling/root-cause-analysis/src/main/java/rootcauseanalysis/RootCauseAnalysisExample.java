package rootcauseanalysis;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import rootcauseanalysis.workers.*;
import java.util.List;
import java.util.Map;

public class RootCauseAnalysisExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 425: Root Cause Analysis ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rca_detect_issue", "rca_collect_evidence", "rca_analyze", "rca_identify_root_cause"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DetectIssueWorker(), new CollectEvidenceWorker(), new RcaAnalyzeWorker(), new IdentifyRootCauseWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("root_cause_analysis_425", 1, Map.of("incidentId","INC-20260308-042","affectedService","checkout-service","symptom","5xx errors spiked to 15%"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
