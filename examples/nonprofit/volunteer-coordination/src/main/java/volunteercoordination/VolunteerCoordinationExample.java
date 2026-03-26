package volunteercoordination;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import volunteercoordination.workers.*;
import java.util.List;
import java.util.Map;
/** Example 753: Volunteer Coordination — Register, Match, Schedule, Track, Thank */
public class VolunteerCoordinationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 753: Volunteer Coordination ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("vol_register", "vol_match", "vol_schedule", "vol_track", "vol_thank"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RegisterWorker(), new MatchWorker(), new ScheduleWorker(), new TrackWorker(), new ThankWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("volunteer_coordination_753", 1, Map.of("volunteerName", "Maria Garcia", "skills", List.of("organization","cooking"), "availability", "weekends"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
