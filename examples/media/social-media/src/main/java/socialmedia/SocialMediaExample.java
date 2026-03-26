package socialmedia;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import socialmedia.workers.CreateContentWorker;
import socialmedia.workers.SchedulePostWorker;
import socialmedia.workers.PublishPostWorker;
import socialmedia.workers.MonitorEngagementWorker;
import socialmedia.workers.EngageResponsesWorker;
import java.util.List;
import java.util.Map;
public class SocialMediaExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 515: Social Media ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("soc_create_content", "soc_schedule_post", "soc_publish_post", "soc_monitor_engagement", "soc_engage_responses"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateContentWorker(), new SchedulePostWorker(), new PublishPostWorker(), new MonitorEngagementWorker(), new EngageResponsesWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("social_media_workflow", 1, Map.of("campaignId", "CAMP-515-Q1", "platform", "twitter", "message", "Automate your workflows with Conductor!", "mediaUrl", "https://media.example.com/promo/515.png"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
