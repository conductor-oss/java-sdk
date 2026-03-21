package securitytraining;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import securitytraining.workers.StAssignTrainingWorker;
import securitytraining.workers.StSendPhishingSimWorker;
import securitytraining.workers.StEvaluateResultsWorker;
import securitytraining.workers.StReportComplianceWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 393: Security Training -- Automated Security Awareness Campaign
 *
 * Pattern:
 *   assign-training -> send-phishing-sim -> evaluate-results -> report-compliance
 */
public class SecurityTrainingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 393: Security Training ===\n");

        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "st_assign_training", "st_send_phishing_sim", "st_evaluate_results", "st_report_compliance"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new StAssignTrainingWorker(),
                new StSendPhishingSimWorker(),
                new StEvaluateResultsWorker(),
                new StReportComplianceWorker()
        );
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = client.startWorkflow("security_training_workflow", 1,
                Map.of("department", "engineering", "trainingModule", "secure-coding-2024"));

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
