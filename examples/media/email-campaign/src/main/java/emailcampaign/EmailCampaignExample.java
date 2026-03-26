package emailcampaign;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import emailcampaign.workers.SegmentAudienceWorker;
import emailcampaign.workers.PersonalizeWorker;
import emailcampaign.workers.SendCampaignWorker;
import emailcampaign.workers.TrackEngagementWorker;
import emailcampaign.workers.AnalyzeResultsWorker;
import java.util.List;
import java.util.Map;
public class EmailCampaignExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 524: Email Campaign ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("eml_segment_audience", "eml_personalize", "eml_send_campaign", "eml_track_engagement", "eml_analyze_results"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SegmentAudienceWorker(), new PersonalizeWorker(), new SendCampaignWorker(), new TrackEngagementWorker(), new AnalyzeResultsWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("email_campaign_workflow", 1, Map.of("campaignId", "CAMP-524-001", "subject", "Unlock the Power of Workflow Automation", "templateId", "TPL-PROMO-03", "listId", "LIST-MAIN-2026"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
