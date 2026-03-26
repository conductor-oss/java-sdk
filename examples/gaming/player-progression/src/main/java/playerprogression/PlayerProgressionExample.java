package playerprogression;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import playerprogression.workers.*;
import java.util.List;
import java.util.Map;
/** Example 745: Player Progression — Complete Task, Award XP, Check Level, Unlock Rewards, Notify */
public class PlayerProgressionExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 745: Player Progression ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ppg_complete_task", "ppg_award_xp", "ppg_check_level", "ppg_unlock_rewards", "ppg_notify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CompleteTaskWorker(), new AwardXpWorker(), new CheckLevelWorker(), new UnlockRewardsWorker(), new NotifyWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("player_progression_745", 1, Map.of("playerId", "P-042", "questId", "Q-DragonsLair", "xpEarned", 500));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
