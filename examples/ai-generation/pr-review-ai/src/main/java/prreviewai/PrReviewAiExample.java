package prreviewai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import prreviewai.workers.*;
import java.util.List;
import java.util.Map;
public class PrReviewAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 647: PR Review AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("prr_fetch_diff", "prr_analyze_changes", "prr_generate_review", "prr_post_review"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new FetchDiffWorker(), new AnalyzeChangesWorker(), new GenerateReviewWorker(), new PostReviewWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("prr_pr_review", 1,
                Map.of("repoName", "acme/backend", "prNumber", 247));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
