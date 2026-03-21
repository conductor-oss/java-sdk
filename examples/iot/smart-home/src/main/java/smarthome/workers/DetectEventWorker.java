package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class DetectEventWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_detect_event"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [detect] Processing " + task.getInputData().getOrDefault("context", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("context", Map.of());
        r.getOutputData().put("occupancy", true);
        r.getOutputData().put("mode", "home");
        r.getOutputData().put("currentTemp", 72);
        return r;
    }
}
