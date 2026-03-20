package populationhealth;

import com.netflix.conductor.client.worker.Worker;
import populationhealth.workers.*;

import java.util.List;
import java.util.Map;

public class PopulationHealthExample {

    private static final List<Worker> WORKERS = List.of(
            new AggregateDataWorker(),
            new StratifyRiskWorker(),
            new IdentifyGapsWorker(),
            new InterveneWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Population Health Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("pop_aggregate_data", "pop_stratify_risk", "pop_identify_gaps", "pop_intervene"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("population_health_workflow", 1, Map.of(
                "cohortId", "COHORT-DM2-2024",
                "condition", "Type 2 Diabetes",
                "reportingPeriod", "2024-Q1"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Population size: " + result.getOutput().get("populationSize"));
        System.out.println("Care gaps: " + result.getOutput().get("careGaps"));
        System.out.println("Interventions: " + result.getOutput().get("interventions"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
