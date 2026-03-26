package anticheat;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import anticheat.workers.*;
import java.util.List;
import java.util.Map;
/** Example 746: Anti-Cheat — Monitor, Detect Anomaly, SWITCH(clean/suspect/cheat), Act */
public class AntiCheatExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 746: Anti-Cheat ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ach_monitor", "ach_detect_anomaly", "ach_clean", "ach_suspect", "ach_cheat", "ach_act"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorWorker(), new DetectAnomalyWorker(), new CleanWorker(), new SuspectWorker(), new CheatWorker(), new ActWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("anti_cheat_746", 1, Map.of("playerId", "P-042", "matchId", "MATCH-999"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
