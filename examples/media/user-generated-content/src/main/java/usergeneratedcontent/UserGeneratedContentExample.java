package usergeneratedcontent;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import usergeneratedcontent.workers.SubmitWorker;
import usergeneratedcontent.workers.ModerateWorker;
import usergeneratedcontent.workers.ApproveWorker;
import usergeneratedcontent.workers.EnrichWorker;
import usergeneratedcontent.workers.PublishWorker;
import java.util.List;
import java.util.Map;
public class UserGeneratedContentExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 527: User Generated Content ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ugc_submit", "ugc_moderate", "ugc_approve", "ugc_enrich", "ugc_publish"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitWorker(), new ModerateWorker(), new ApproveWorker(), new EnrichWorker(), new PublishWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("user_generated_content_workflow", 1, Map.of("submissionId", "UGC-527-001", "userId", "USER-5270", "contentType", "review", "contentBody", "Conductor has transformed how we build our microservices. Highly recommended!", "title", "Amazing Workflow Platform"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
