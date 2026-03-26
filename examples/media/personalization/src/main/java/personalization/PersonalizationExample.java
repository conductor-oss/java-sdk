package personalization;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import personalization.workers.CollectProfileWorker;
import personalization.workers.SegmentUserWorker;
import personalization.workers.SelectContentWorker;
import personalization.workers.RankContentWorker;
import personalization.workers.ServeContentWorker;
import java.util.List;
import java.util.Map;
public class PersonalizationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 519: Personalization ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("per_collect_profile", "per_segment_user", "per_select_content", "per_rank_content", "per_serve_content"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectProfileWorker(), new SegmentUserWorker(), new SelectContentWorker(), new RankContentWorker(), new ServeContentWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("personalization_workflow", 1, Map.of("userId", "USER-519-001", "sessionId", "SESS-ABC123", "pageContext", "homepage"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
