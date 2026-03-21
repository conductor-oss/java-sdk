package oncallrotation;

import com.netflix.conductor.client.worker.Worker;
import oncallrotation.workers.CheckScheduleWorker;
import oncallrotation.workers.HandoffWorker;
import oncallrotation.workers.UpdateRoutingWorker;
import oncallrotation.workers.ConfirmWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 524: On-Call Rotation — Automated On-Call Schedule Management
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 524: On-Call Rotation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "oc_check_schedule",
                "oc_handoff",
                "oc_update_routing",
                "oc_confirm"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CheckScheduleWorker(),
                new HandoffWorker(),
                new UpdateRoutingWorker(),
                new ConfirmWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("on_call_rotation_workflow", 1, Map.of(
                "team", "platform-eng",
                "rotationType", "weekly"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  check_scheduleResult: " + execution.getOutput().get("check_scheduleResult"));
        System.out.println("  confirmResult: " + execution.getOutput().get("confirmResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
