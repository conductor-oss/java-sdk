package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ActuateThermostatWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_actuate_thermostat"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [thermostat] Processing " + task.getInputData().getOrDefault("actuated", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("actuated", true);
        r.getOutputData().put("device", "thermostat");
        r.getOutputData().put("setTemp", task.getInputData().get("targetTemp"));
        return r;
    }
}
