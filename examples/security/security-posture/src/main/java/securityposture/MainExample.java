package securityposture;

import com.netflix.conductor.client.worker.Worker;
import securityposture.workers.AssessInfrastructureWorker;
import securityposture.workers.AssessApplicationWorker;
import securityposture.workers.AssessComplianceWorker;
import securityposture.workers.CalculateScoreWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 400: Security Posture — Unified Security Posture Assessment
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 400: Security Posture ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "sp_assess_infrastructure",
                "sp_assess_application",
                "sp_assess_compliance",
                "sp_calculate_score"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new AssessInfrastructureWorker(),
                new AssessApplicationWorker(),
                new AssessComplianceWorker(),
                new CalculateScoreWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("security_posture_workflow", 1, Map.of(
                "organization", "acme-corp",
                "assessmentScope", "full"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  assess_infrastructureResult: " + execution.getOutput().get("assess_infrastructureResult"));
        System.out.println("  calculate_scoreResult: " + execution.getOutput().get("calculate_scoreResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
