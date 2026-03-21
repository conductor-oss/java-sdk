package fundraisingcampaign;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import fundraisingcampaign.workers.*;
import java.util.List;
import java.util.Map;
/** Example 754: Fundraising Campaign — Plan, Launch, Track, Close, Report */
public class FundraisingCampaignExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 754: Fundraising Campaign ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("frc_plan", "frc_launch", "frc_track", "frc_close", "frc_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PlanWorker(), new LaunchWorker(), new TrackWorker(), new CloseWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("fundraising_campaign_754", 1, Map.of("campaignName", "Spring Hope Drive", "goalAmount", 100000, "endDate", "2026-04-30"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
