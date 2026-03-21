package commitanalysis;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import commitanalysis.workers.*;
import java.util.List;
import java.util.Map;
public class CommitAnalysisExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 646: Commit Analysis ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cma_parse_commits", "cma_classify", "cma_detect_patterns", "cma_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseCommitsWorker(), new ClassifyWorker(), new DetectPatternsWorker(), new ReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cma_commit_analysis", 1,
                Map.of("repoName", "acme/platform", "branch", "main", "days", 30));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
