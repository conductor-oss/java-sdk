package tournamentbracket;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tournamentbracket.workers.*;
import java.util.List;
import java.util.Map;
/** Example 743: Tournament Bracket — Register, Seed, Create Bracket, Manage Rounds, Finalize */
public class TournamentBracketExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 743: Tournament Bracket ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("tbk_register", "tbk_seed", "tbk_create_bracket", "tbk_manage_rounds", "tbk_finalize"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RegisterWorker(), new SeedWorker(), new CreateBracketWorker(), new ManageRoundsWorker(), new FinalizeWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("tournament_bracket_743", 1, Map.of("tournamentName", "Spring Showdown 2026", "format", "single-elimination", "maxPlayers", 8));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
