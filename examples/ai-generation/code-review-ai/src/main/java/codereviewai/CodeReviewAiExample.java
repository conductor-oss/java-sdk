package codereviewai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import codereviewai.workers.*;
import java.util.List;
import java.util.Map;
public class CodeReviewAiExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 640: AI Code Review ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cra_parse_diff", "cra_security_check", "cra_quality_check", "cra_style_check", "cra_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseDiffWorker(), new SecurityCheckWorker(), new QualityCheckWorker(), new StyleCheckWorker(), new ReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cra_code_review_ai", 1, Map.of("prUrl", "https://github.com/example/repo/pull/42", "diff", "+md5(pw)"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
