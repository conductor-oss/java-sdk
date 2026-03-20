package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ActuateSecurityWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_actuate_security"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [security] Processing " + task.getInputData().getOrDefault("actuated", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("actuated", true);
        r.getOutputData().put("device", "security_system");
        r.getOutputData().put("zone", task.getInputData().get("zone"));
        return r;
    }
}
