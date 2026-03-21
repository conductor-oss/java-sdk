package environmentalmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "env_collect_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing " + task.getInputData().getOrDefault("readings", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("readings", Map.of());
        r.getOutputData().put("pm25", 35.2);
        r.getOutputData().put("pm10", 48.7);
        r.getOutputData().put("co2", 420);
        r.getOutputData().put("no2", 22.5);
        r.getOutputData().put("ozone", 0.065);
        r.getOutputData().put("temperature", 28.5);
        r.getOutputData().put("humidity", 62);
        return r;
    }
}
