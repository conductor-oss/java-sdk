package releasenotesai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import releasenotesai.workers.*;
import java.util.List;
import java.util.Map;
public class ReleaseNotesAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 643: Release Notes AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rna_collect_commits", "rna_categorize", "rna_generate_notes", "rna_publish"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectCommitsWorker(), new CategorizeWorker(), new GenerateNotesWorker(), new PublishWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rna_release_notes", 1,
                Map.of("repoName", "acme/backend", "fromTag", "v2.3.0", "toTag", "v2.4.0"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
