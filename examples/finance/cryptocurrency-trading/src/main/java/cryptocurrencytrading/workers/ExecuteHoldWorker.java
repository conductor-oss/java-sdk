package cryptocurrencytrading.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExecuteHoldWorker implements Worker {
    @Override public String getTaskDefName() { return "cry_execute_hold"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [hold] Holding " + task.getInputData().get("pair") + ": " + task.getInputData().get("reason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "hold"); r.getOutputData().put("reason", task.getInputData().get("reason"));
        r.getOutputData().put("reviewAt", "2026-03-09T10:00:00Z");
        return r;
    }
}
