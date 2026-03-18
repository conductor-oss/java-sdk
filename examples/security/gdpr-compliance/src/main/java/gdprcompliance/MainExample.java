package gdprcompliance;

import com.netflix.conductor.client.worker.Worker;
import gdprcompliance.workers.VerifyIdentityWorker;
import gdprcompliance.workers.LocateDataWorker;
import gdprcompliance.workers.ProcessRequestWorker;
import gdprcompliance.workers.ConfirmCompletionWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 357: GDPR Compliance — Data Subject Request Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 357: GDPR Compliance ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "gdpr_verify_identity",
                "gdpr_locate_data",
                "gdpr_process_request",
                "gdpr_confirm_completion"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new VerifyIdentityWorker(),
                new LocateDataWorker(),
                new ProcessRequestWorker(),
                new ConfirmCompletionWorker()
        );
        helper.startWorkers(workers);


        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = helper.startWorkflow("gdpr_compliance_workflow", 1, Map.of(
                "subjectId", "EU-USER-12345",
                "requestType", "right-to-erasure"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  verify_identityResult: " + execution.getOutput().get("verify_identityResult"));
        System.out.println("  confirm_completionResult: " + execution.getOutput().get("confirm_completionResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
