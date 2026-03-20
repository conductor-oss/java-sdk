package timetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessWorker implements Worker {
    @Override public String getTaskDefName() { return "ttk_process"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] " + task.getInputData().get("totalHours") + " hours sent to payroll");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("payrollBatch", "PAY-2024-W12");
        return result;
    }
}
