package votingworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import votingworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 321: Voting Workflow — Register, Verify Identity, Cast Ballot, Count, Certify
 *
 * Runs an electronic voting process from registration to certification.
 */
public class VotingWorkflowExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 321: Voting Workflow ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("vtw_register", "vtw_verify_identity", "vtw_cast_ballot", "vtw_count", "vtw_certify"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new RegisterWorker(),
                new VerifyIdentityWorker(),
                new CastBallotWorker(),
                new CountWorker(),
                new CertifyWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("vtw_voting_workflow", 1, Map.of(
                    "voterId", "VTR-300",
                    "electionId", "ELEC-2024",
                    "precinct", "PCT-12"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Certified: %s%n", workflow.getOutput().get("certified"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
