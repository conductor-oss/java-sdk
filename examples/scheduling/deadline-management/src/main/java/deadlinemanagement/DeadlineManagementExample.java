package deadlinemanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import deadlinemanagement.workers.*;
import java.util.List;
import java.util.Map;

public class DeadlineManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 410: Deadline Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ded_check_deadlines", "ded_handle_urgent", "ded_handle_normal", "ded_handle_overdue"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckDeadlinesWorker(), new HandleUrgentWorker(), new HandleNormalWorker(), new HandleOverdueWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("deadline_management_410", 1, Map.of("projectId","proj-alpha","taskId","TASK-2201","dueDate","2026-03-08T18:00:00Z"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
