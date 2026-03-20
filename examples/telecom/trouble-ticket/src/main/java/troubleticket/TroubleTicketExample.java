package troubleticket;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import troubleticket.workers.OpenWorker;
import troubleticket.workers.DiagnoseWorker;
import troubleticket.workers.AssignWorker;
import troubleticket.workers.ResolveWorker;
import troubleticket.workers.CloseWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 767: Trouble Ticket
 */
public class TroubleTicketExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 767: Trouble Ticket ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("tbt_open", "tbt_diagnose", "tbt_assign", "tbt_resolve", "tbt_close"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OpenWorker(), new DiagnoseWorker(), new AssignWorker(), new ResolveWorker(), new CloseWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("tbt_trouble_ticket", 1, Map.of("customerId", "CUST-767", "issueType", "no-service", "description", "Internet down since yesterday"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("resolution: %s%n", workflow.getOutput().get("resolution"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
