package governmentpermit;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import governmentpermit.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 521: Government Permit — Apply, Validate, Review, Approve/Deny, Issue
 *
 * Performs a government permit application workflow with conditional
 * approval or denial via SWITCH.
 */
public class GovernmentPermitExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 521: Government Permit ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        // Register task definitions
        helper.registerTaskDefs(List.of("gvp_apply", "gvp_validate", "gvp_review", "gvp_issue", "gvp_deny"));

        // Register workflow from JSON
        helper.registerWorkflow("workflow.json");

        // Start workers
        List<Worker> workers = List.of(
                new ApplyWorker(),
                new ValidateWorker(),
                new ReviewWorker(),
                new IssueWorker(),
                new DenyWorker()
        );
        helper.startWorkers(workers);

        try {
            // Start workflow
            String workflowId = helper.startWorkflow("gvp_government_permit", 1, Map.of(
                    "applicantId", "CIT-100",
                    "permitType", "building",
                    "details", "New garage construction"
            ));

            // Wait for completion
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Decision: %s%n", workflow.getOutput().get("decision"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
