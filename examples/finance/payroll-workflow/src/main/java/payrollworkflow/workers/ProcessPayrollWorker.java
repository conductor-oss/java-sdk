package payrollworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessPayrollWorker implements Worker {
    @Override public String getTaskDefName() { return "prl_process_payroll"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Processing net payroll: $" + task.getInputData().get("netPayroll"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("batchId", "PRL-BATCH-507-001"); r.getOutputData().put("processedAt", "2026-03-08T14:00:00Z");
        r.getOutputData().put("bankReference", "ACH-2026030801");
        return r;
    }
}
