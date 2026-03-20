package eventmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventmanagement.workers.*;
import java.util.List;
import java.util.Map;

public class EventManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 627: Event Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("evt_plan", "evt_register", "evt_execute", "evt_followup"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PlanWorker(), new RegisterAttendeesWorker(), new ExecuteEventWorker(), new FollowupWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("evt_event_management", 1,
                Map.of("eventName", "DevOps Summit 2024", "date", "2024-11-15", "capacity", 200));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
