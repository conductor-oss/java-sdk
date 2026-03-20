package knowledgebasesync;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import knowledgebasesync.workers.*;
import java.util.List;
import java.util.Map;

public class KnowledgeBaseSyncExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 630: Knowledge Base Sync ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("kbs_crawl", "kbs_extract", "kbs_update", "kbs_index", "kbs_verify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CrawlWorker(), new ExtractWorker(), new UpdateWorker(), new IndexWorker(), new VerifyWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("kbs_knowledge_base_sync", 1,
                Map.of("sourceUrl", "https://docs.example.com", "kbId", "KB-MAIN-001"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
