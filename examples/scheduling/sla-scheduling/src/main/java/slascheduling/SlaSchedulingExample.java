package slascheduling;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import slascheduling.workers.*;
import java.util.List;
import java.util.Map;

public class SlaSchedulingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 409: SLA Scheduling ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sla_prioritize", "sla_execute_tasks", "sla_track_compliance"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new PrioritizeWorker(), new ExecuteTasksWorker(), new TrackComplianceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("sla_scheduling_409", 1, Map.of("tickets", List.of("TKT-101","TKT-102","TKT-103"), "slaPolicy", "enterprise-premium"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
