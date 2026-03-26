package payrollworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import payrollworkflow.workers.*;
import java.util.List;
import java.util.Map;
public class PayrollWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 507: Payroll Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("prl_collect_hours","prl_calculate_gross","prl_apply_deductions","prl_process_payroll","prl_distribute_stubs"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectHoursWorker(), new CalculateGrossWorker(), new ApplyDeductionsWorker(), new ProcessPayrollWorker(), new DistributeStubsWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("payroll_workflow", 1, Map.of("payPeriodId","PP-2026-05","departmentId","DEPT-ENG"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
