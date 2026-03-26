package expensereporting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import expensereporting.workers.*;
import java.util.List; import java.util.Map;
public class ExpenseReportingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 543: Expense Reporting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("exr_collect","exr_categorize","exr_submit","exr_approve","exr_reimburse"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectWorker(),new CategorizeWorker(),new SubmitWorker(),new ApproveWorker(),new ReimburseWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("exr_expense_reporting", 1, Map.of("employeeId","EMP-900","tripId","TRIP-NYC-2024"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Report ID: " + wf.getOutput().get("reportId"));
        System.out.println("  Total: " + wf.getOutput().get("totalAmount"));
        System.out.println("  Reimbursed: " + wf.getOutput().get("reimbursed"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
