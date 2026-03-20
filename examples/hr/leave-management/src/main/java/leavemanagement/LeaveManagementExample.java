package leavemanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import leavemanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 601: Leave Management
 *
 * Handles employee leave request workflow with balance checking.
 */
public class LeaveManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 601: Leave Management ===\n");

        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "lvm_request", "lvm_check_balance", "lvm_approve", "lvm_update", "lvm_notify"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new RequestWorker(), new CheckBalanceWorker(), new ApproveWorker(),
                new UpdateWorker(), new NotifyWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("lvm_leave_management", 1,
                Map.of("employeeId", "EMP-400", "leaveType", "vacation",
                       "startDate", "2024-04-15", "days", 5));
        System.out.println("  Workflow ID: " + workflowId);

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Request ID: " + workflow.getOutput().get("requestId"));
        System.out.println("  Approved: " + workflow.getOutput().get("approved"));
        System.out.println("  Remaining balance: " + workflow.getOutput().get("remainingBalance"));

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
