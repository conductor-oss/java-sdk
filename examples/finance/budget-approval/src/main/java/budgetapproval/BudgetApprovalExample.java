package budgetapproval;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import budgetapproval.workers.*;
import java.util.List;
import java.util.Map;
public class BudgetApprovalExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 504: Budget Approval ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bgt_submit_budget","bgt_review_budget","bgt_approve_budget","bgt_revise_budget","bgt_reject_budget","bgt_allocate_funds"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitBudgetWorker(), new ReviewBudgetWorker(), new ApproveBudgetWorker(), new ReviseBudgetWorker(), new RejectBudgetWorker(), new AllocateFundsWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("budget_approval_workflow", 1, Map.of("budgetId","BDG-2026-Q2-ENG","department","engineering","amount",45000,"justification","Cloud infrastructure expansion"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
