package programevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectWorker implements Worker {
    @Override public String getTaskDefName() { return "pev_collect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collecting data for period: " + task.getInputData().get("period"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("collected", Map.of("reach", 5000, "outcomes", 82, "costPer", 35, "satisfaction", 4.6)); return r;
    }
}
