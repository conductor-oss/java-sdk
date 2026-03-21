package dripcampaign;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dripcampaign.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 625: Drip Campaign
 * drp_enroll -> drp_send_series -> drp_track_engagement -> drp_graduate
 */
public class DripCampaignExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 625: Drip Campaign ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("drp_enroll", "drp_send_series", "drp_track_engagement", "drp_graduate"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new EnrollWorker(), new SendSeriesWorker(), new TrackEngagementWorker(), new GraduateWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("drp_drip_campaign", 1,
                Map.of("contactId", "CON-DRP01", "campaignId", "DRP-ONBOARD-2024", "email", "iris@startup.io"));
        System.out.println("  Workflow ID: " + workflowId);
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
