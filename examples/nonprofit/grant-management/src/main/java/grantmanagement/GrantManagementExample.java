package grantmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import grantmanagement.workers.*;
import java.util.List;
import java.util.Map;
/** Example 752: Grant Management — Apply, Review, Approve, Fund, Report */
public class GrantManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 752: Grant Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("gmt_apply", "gmt_review", "gmt_approve", "gmt_fund", "gmt_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ApplyWorker(), new ReviewWorker(), new ApproveWorker(), new FundWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("grant_management_752", 1, Map.of("organizationName", "HopeWorks Foundation", "grantProgram", "Education Access", "requestedAmount", 50000));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
