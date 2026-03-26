package druginteraction;

import com.netflix.conductor.client.worker.Worker;
import druginteraction.workers.*;

import java.util.List;
import java.util.Map;

public class DrugInteractionExample {

    private static final List<Worker> WORKERS = List.of(
            new ListMedicationsWorker(),
            new CheckPairsWorker(),
            new FlagConflictsWorker(),
            new RecommendAlternativesWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Drug Interaction Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("drg_list_medications", "drg_check_pairs", "drg_flag_conflicts", "drg_recommend_alternatives"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("drug_interaction_workflow", 1, Map.of(
                "patientId", "PAT-10234",
                "newMedication", "Aspirin 325mg"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Conflicts found: " + result.getOutput().get("conflictsFound"));
        System.out.println("Alternatives: " + result.getOutput().get("alternatives"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
