package seasonmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import seasonmanagement.workers.*;
import java.util.List;
import java.util.Map;
/** Example 749: Season Management — Create Season, Define Rewards, Launch, Track, Close */
public class SeasonManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 749: Season Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("smg_create_season", "smg_define_rewards", "smg_launch", "smg_track", "smg_close"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateSeasonWorker(), new DefineRewardsWorker(), new LaunchWorker(), new TrackWorker(), new CloseWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("season_management_749", 1, Map.of("seasonNumber", 3, "theme", "Frozen Frontier", "durationWeeks", 10));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
