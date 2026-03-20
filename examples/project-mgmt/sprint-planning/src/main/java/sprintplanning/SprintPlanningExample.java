package sprintplanning;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sprintplanning.workers.*;
import java.util.List; import java.util.Map;
public class SprintPlanningExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example sprint-planning: Sprint Planning ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("spn_select_stories","spn_estimate","spn_assign","spn_create_sprint"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SelectStoriesWorker(),new EstimateWorker(),new AssignWorker(),new CreateSprintWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("sprint_planning_sprint-planning", 1, Map.of("sprintNumber",14,"teamCapacity",20));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
