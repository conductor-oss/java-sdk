package environmentalmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckThresholdsWorker implements Worker {
    @Override public String getTaskDefName() { return "env_check_thresholds"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [threshold] Processing " + task.getInputData().getOrDefault("breachCount", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("breachCount", "breaches.length");
        r.getOutputData().put("aqi", 105);
        return r;
    }
}
