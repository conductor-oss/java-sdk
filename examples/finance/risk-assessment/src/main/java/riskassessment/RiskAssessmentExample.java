package riskassessment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import riskassessment.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Risk Assessment Workflow Demo
 *
 * Demonstrates parallel risk analysis via FORK_JOIN:
 *   rsk_collect_factors -> FORK(rsk_market_risk, rsk_credit_risk, rsk_operational_risk) -> JOIN -> rsk_aggregate
 *
 * Run:
 *   java -jar target/risk-assessment-1.0.0.jar
 */
public class RiskAssessmentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 495: Risk Assessment ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rsk_collect_factors", "rsk_market_risk", "rsk_credit_risk",
                "rsk_operational_risk", "rsk_aggregate"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'risk_assessment_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectFactorsWorker(),
                new MarketRiskWorker(),
                new CreditRiskWorker(),
                new OperationalRiskWorker(),
                new AggregateWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("risk_assessment_workflow", 1,
                Map.of("portfolioId", "PORT-INST-001",
                       "assessmentDate", "2024-03-15"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
