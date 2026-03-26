package ticketmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ticketmanagement.workers.*;
import java.util.List;
import java.util.Map;

public class TicketManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 628: Ticket Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tkt_create", "tkt_classify", "tkt_assign", "tkt_resolve", "tkt_close"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateTicketWorker(), new ClassifyTicketWorker(), new AssignTicketWorker(), new ResolveTicketWorker(), new CloseTicketWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tkt_ticket_management", 1,
                Map.of("subject", "Cannot login after password reset", "description", "Users cannot login after resetting password - urgent fix needed", "reportedBy", "support@example.com"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
