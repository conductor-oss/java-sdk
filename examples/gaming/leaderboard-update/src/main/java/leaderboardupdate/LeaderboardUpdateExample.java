package leaderboardupdate;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import leaderboardupdate.workers.*;
import java.util.List;
import java.util.Map;
/** Example 742: Leaderboard Update — Collect Scores, Validate, Rank, Update, Broadcast */
public class LeaderboardUpdateExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 742: Leaderboard Update ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("lbu_collect_scores", "lbu_validate", "lbu_rank", "lbu_update", "lbu_broadcast"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectScoresWorker(), new ValidateWorker(), new RankWorker(), new UpdateWorker(), new BroadcastWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("leaderboard_update_742", 1, Map.of("gameId", "GAME-01", "season", "S3"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
