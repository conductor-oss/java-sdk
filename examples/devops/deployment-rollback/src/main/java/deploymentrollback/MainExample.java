package deploymentrollback;

import com.netflix.conductor.client.worker.Worker;
import deploymentrollback.workers.DetectFailureWorker;
import deploymentrollback.workers.IdentifyVersionWorker;
import deploymentrollback.workers.RollbackDeployWorker;
import deploymentrollback.workers.VerifyRollbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 335: Deployment Rollback — Automated Rollback Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 335: Deployment Rollback ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "rb_detect_failure",
                "rb_identify_version",
                "rb_rollback_deploy",
                "rb_verify_rollback"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DetectFailureWorker(),
                new IdentifyVersionWorker(),
                new RollbackDeployWorker(),
                new VerifyRollbackWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("deployment_rollback_workflow", 1, Map.of(
                "service", "checkout-service",
                "reason", "error-rate-spike"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  detect_failureResult: " + execution.getOutput().get("detect_failureResult"));
        System.out.println("  verify_rollbackResult: " + execution.getOutput().get("verify_rollbackResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
