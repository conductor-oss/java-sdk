package gamingmatchmaking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gamingmatchmaking.workers.*;
import java.util.List;
import java.util.Map;

/** Example 741: Gaming Matchmaking — Search Players, Rate Skill, Match, Create Lobby, Start */
public class GamingMatchmakingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 741: Gaming Matchmaking ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("gmm_search_players", "gmm_rate_skill", "gmm_match", "gmm_create_lobby", "gmm_start"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SearchPlayersWorker(), new RateSkillWorker(), new MatchWorker(), new CreateLobbyWorker(), new StartWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("gaming_matchmaking_741", 1, Map.of("playerId", "P-042", "gameMode", "ranked", "region", "NA"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
