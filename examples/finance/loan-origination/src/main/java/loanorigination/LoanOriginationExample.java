package loanorigination;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import loanorigination.workers.ApplicationWorker;
import loanorigination.workers.CreditCheckWorker;
import loanorigination.workers.UnderwriteWorker;
import loanorigination.workers.ApproveWorker;
import loanorigination.workers.FundWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 491: Loan Origination
 *
 * Process a loan application: collect info, run credit check,
 * underwrite the loan, approve it, and fund the disbursement.
 *
 * Run:
 *   java -jar target/loan-origination-1.0.0.jar
 */
public class LoanOriginationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 491: Loan Origination ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lnr_application", "lnr_credit_check", "lnr_underwrite",
                "lnr_approve", "lnr_fund"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'loan_origination_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ApplicationWorker(),
                new CreditCheckWorker(),
                new UnderwriteWorker(),
                new ApproveWorker(),
                new FundWorker()
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
        String workflowId = client.startWorkflow("loan_origination_workflow", 1,
                Map.of("applicationId", "LOAN-2024-0891",
                        "applicantId", "APP-5501",
                        "loanAmount", 500000,
                        "loanType", "mortgage"));
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
