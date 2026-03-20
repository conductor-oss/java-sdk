package expensemanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import expensemanagement.workers.*;
import java.util.List;
import java.util.Map;
public class ExpenseManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 505: Expense Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("exp_submit_expense","exp_validate_receipts","exp_categorize","exp_approve_expense","exp_reimburse"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new SubmitExpenseWorker(), new ValidateReceiptsWorker(), new CategorizeWorker(), new ApproveExpenseWorker(), new ReimburseWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("expense_management_workflow", 1, Map.of("expenseId","EXP-7890","employeeId","EMP-1234","amount",342.50,"category","office_supplies","receiptUrl","https://receipts.example.com/r/7890.pdf"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
