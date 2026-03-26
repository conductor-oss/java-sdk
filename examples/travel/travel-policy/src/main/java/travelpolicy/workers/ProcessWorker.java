package travelpolicy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessWorker implements Worker {
    @Override public String getTaskDefName() { return "tpl_process"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Booking processed for " + task.getInputData().get("employeeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("processed", true); return r;
    }
}
