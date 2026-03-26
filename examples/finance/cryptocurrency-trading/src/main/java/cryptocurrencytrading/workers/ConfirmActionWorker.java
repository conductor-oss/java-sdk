package cryptocurrencytrading.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConfirmActionWorker implements Worker {
    @Override public String getTaskDefName() { return "cry_confirm_action"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Action confirmed: " + task.getInputData().get("signal") + " on " + task.getInputData().get("pair"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true); r.getOutputData().put("portfolioUpdated", true); r.getOutputData().put("logId", "LOG-510-001");
        return r;
    }
}
