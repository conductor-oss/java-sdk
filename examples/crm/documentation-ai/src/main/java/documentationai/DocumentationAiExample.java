package documentationai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import documentationai.workers.*;
import java.util.List;
import java.util.Map;
public class DocumentationAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 645: Documentation AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("doc_analyze_code", "doc_generate_docs", "doc_format", "doc_publish"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AnalyzeCodeWorker(), new GenerateDocsWorker(), new FormatWorker(), new PublishWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("doc_documentation_ai", 1,
                Map.of("repoPath", "acme/platform", "outputFormat", "markdown"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
