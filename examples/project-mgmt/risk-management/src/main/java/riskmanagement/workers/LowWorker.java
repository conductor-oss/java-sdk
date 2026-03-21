package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class LowWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_low"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [low] LOW severity — monitor and log");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "monitor"); return r;
    }
}
