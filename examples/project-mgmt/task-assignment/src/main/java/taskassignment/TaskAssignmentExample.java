package taskassignment;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskassignment.workers.*;
import java.util.List; import java.util.Map;
public class TaskAssignmentExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example task-assignment: Task Assignment ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tas_analyze","tas_match_skills","tas_assign","tas_notify","tas_track"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AnalyzeWorker(),new MatchSkillsWorker(),new AssignWorker(),new NotifyWorker(),new TrackWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("task_assignment_task-assignment", 1, Map.of("taskTitle","Build search feature","requiredSkills",List.of("JavaScript","React"),"priority","high"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
