package campaignautomation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import campaignautomation.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 624: Campaign Automation
 * cpa_design -> cpa_target -> cpa_execute -> cpa_measure
 */
public class CampaignAutomationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 624: Campaign Automation ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of("cpa_design", "cpa_target", "cpa_execute", "cpa_measure"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new DesignWorker(), new TargetWorker(), new ExecuteWorker(), new MeasureWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("cpa_campaign_automation", 1,
                Map.of("campaignName", "Winter Product Launch", "type", "multi-channel", "budget", 10000));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
