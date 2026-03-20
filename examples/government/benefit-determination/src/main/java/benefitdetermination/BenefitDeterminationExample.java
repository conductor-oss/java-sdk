package benefitdetermination;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import benefitdetermination.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 529: Benefit Determination — Apply, Verify Eligibility, Calculate, SWITCH, Notify
 *
 * Performs a government benefits determination with eligibility check via SWITCH.
 */
public class BenefitDeterminationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 529: Benefit Determination ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("bnd_apply", "bnd_verify_eligibility", "bnd_calculate", "bnd_notify_eligible", "bnd_notify_ineligible"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ApplyWorker(),
                new VerifyEligibilityWorker(),
                new CalculateWorker(),
                new NotifyEligibleWorker(),
                new NotifyIneligibleWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("bnd_benefit_determination", 1, Map.of(
                    "applicantId", "CIT-529",
                    "programType", "housing-assistance",
                    "income", 50000
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Eligibility: %s%n", workflow.getOutput().get("eligibility"));
            System.out.printf("Benefit amount: %s%n", workflow.getOutput().get("benefitAmount"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
