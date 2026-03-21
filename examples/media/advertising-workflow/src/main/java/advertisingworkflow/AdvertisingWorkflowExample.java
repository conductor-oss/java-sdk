package advertisingworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import advertisingworkflow.workers.CreateCampaignWorker;
import advertisingworkflow.workers.TargetAudienceWorker;
import advertisingworkflow.workers.SetBidsWorker;
import advertisingworkflow.workers.ServeAdsWorker;
import advertisingworkflow.workers.GenerateReportWorker;
import java.util.List;
import java.util.Map;
public class AdvertisingWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 530: Advertising Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("adv_create_campaign", "adv_target_audience", "adv_set_bids", "adv_serve_ads", "adv_generate_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateCampaignWorker(), new TargetAudienceWorker(), new SetBidsWorker(), new ServeAdsWorker(), new GenerateReportWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("advertising_workflow", 1, Map.of("campaignId", "ADC-530-Q1", "advertiserId", "ADV-100", "budget", 10000, "objective", "conversions"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
