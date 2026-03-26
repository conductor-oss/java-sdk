package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ActuateLightsWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_actuate_lights"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lights] Processing " + task.getInputData().getOrDefault("actuated", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("actuated", true);
        r.getOutputData().put("device", "lights");
        r.getOutputData().put("brightness", 80);
        r.getOutputData().put("color", "warm_white");
        return r;
    }
}
