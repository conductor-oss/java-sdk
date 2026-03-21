package contentmoderation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentmoderation.workers.SubmitContentWorker;
import contentmoderation.workers.AutoCheckWorker;
import contentmoderation.workers.ApproveSafeWorker;
import contentmoderation.workers.HumanReviewWorker;
import contentmoderation.workers.BlockContentWorker;
import contentmoderation.workers.FinalizeWorker;
import java.util.List;
import java.util.Map;
public class ContentModerationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 516: Content Moderation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mod_submit_content", "mod_auto_check", "mod_approve_safe", "mod_human_review", "mod_block_content", "mod_finalize"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitContentWorker(), new AutoCheckWorker(), new ApproveSafeWorker(), new HumanReviewWorker(), new BlockContentWorker(), new FinalizeWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("content_moderation_workflow", 1, Map.of("contentId", "UGC-516-001", "contentType", "comment", "userId", "USER-8800", "contentText", "Great article! Really enjoyed the insights on workflow automation."));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
