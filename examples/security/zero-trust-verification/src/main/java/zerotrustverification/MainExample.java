package zerotrustverification;

import com.netflix.conductor.client.worker.Worker;
import zerotrustverification.workers.VerifyIdentityWorker;
import zerotrustverification.workers.AssessDeviceWorker;
import zerotrustverification.workers.EvaluateContextWorker;
import zerotrustverification.workers.EnforcePolicyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 366: Zero Trust Verification — Continuous Trust Assessment
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 366: Zero Trust Verification ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "zt_verify_identity",
                "zt_assess_device",
                "zt_evaluate_context",
                "zt_enforce_policy"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new VerifyIdentityWorker(),
                new AssessDeviceWorker(),
                new EvaluateContextWorker(),
                new EnforcePolicyWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("zero_trust_verification_workflow", 1, Map.of(
                "userId", "engineer-01",
                "deviceId", "LAPTOP-A1B2C3",
                "requestedResource", "production-api"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  verify_identityResult: " + execution.getOutput().get("verify_identityResult"));
        System.out.println("  enforce_policyResult: " + execution.getOutput().get("enforce_policyResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
