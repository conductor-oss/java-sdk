package contentsyndication;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentsyndication.workers.SelectContentWorker;
import contentsyndication.workers.FormatPerPlatformWorker;
import contentsyndication.workers.DistributeWorker;
import contentsyndication.workers.TrackPerformanceWorker;
import java.util.List;
import java.util.Map;
public class ContentSyndicationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 523: Content Syndication ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("syn_select_content", "syn_format_per_platform", "syn_distribute", "syn_track_performance"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SelectContentWorker(), new FormatPerPlatformWorker(), new DistributeWorker(), new TrackPerformanceWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("content_syndication_workflow", 1, Map.of("contentId", "CNT-523-001", "title", "Workflow Orchestration Best Practices", "platforms", List.of("medium", "devto", "hashnode"), "publishDate", "2026-03-08"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
