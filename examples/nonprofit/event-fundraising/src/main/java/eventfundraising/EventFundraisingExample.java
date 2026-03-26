package eventfundraising;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventfundraising.workers.*;
import java.util.List;
import java.util.Map;
/** Example 759: Event Fundraising — Plan, Promote, Execute, Collect, Reconcile */
public class EventFundraisingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 759: Event Fundraising ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("efr_plan", "efr_promote", "efr_execute", "efr_collect", "efr_reconcile"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PlanWorker(), new PromoteWorker(), new ExecuteWorker(), new CollectWorker(), new ReconcileWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("event_fundraising_759", 1, Map.of("eventName", "Gala for Good", "eventDate", "2026-05-20", "ticketPrice", 150));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
