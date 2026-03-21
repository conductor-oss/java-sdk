package helpdeskrouting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import helpdeskrouting.workers.*;
import java.util.List;
import java.util.Map;

public class HelpdeskRoutingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 629: Helpdesk Routing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("hdr_classify", "hdr_tier1", "hdr_tier2", "hdr_tier3"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ClassifyWorker(), new Tier1Worker(), new Tier2Worker(), new Tier3Worker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("hdr_helpdesk_routing", 1,
                Map.of("issueDescription", "API integration returning 500 errors on bulk requests", "customerId", "CUST-HDR01"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
