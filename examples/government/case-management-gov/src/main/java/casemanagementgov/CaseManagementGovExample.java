package casemanagementgov;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import casemanagementgov.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 522: Case Management (Gov) — Open Case, Investigate, Evaluate, Decide, Close
 *
 * Performs a government case management workflow from opening to closure.
 */
public class CaseManagementGovExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 522: Case Management (Gov) ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("cmg_open_case", "cmg_investigate", "cmg_evaluate", "cmg_decide", "cmg_close"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new OpenCaseWorker(),
                new InvestigateWorker(),
                new EvaluateWorker(),
                new DecideWorker(),
                new CloseWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("cmg_case_management_gov", 1, Map.of(
                    "caseType", "regulatory-violation",
                    "reporterId", "INSP-10",
                    "description", "Non-compliant waste disposal"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Decision: %s%n", workflow.getOutput().get("decision"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
