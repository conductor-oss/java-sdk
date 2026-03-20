package predictivemaintenance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "pmn_collect_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing " + task.getInputData().getOrDefault("operationalData", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("operationalData", Map.of());
        r.getOutputData().put("currentTemp", 178);
        r.getOutputData().put("vibrationLevel", 3.8);
        r.getOutputData().put("operatingHours", 18500);
        r.getOutputData().put("lastMaintenanceHours", 16000);
        return r;
    }
}
