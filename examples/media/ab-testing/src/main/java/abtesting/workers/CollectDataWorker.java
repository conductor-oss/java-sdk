package abtesting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "abt_collect_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Processing " + task.getInputData().getOrDefault("metricsA", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metricsA", Map.of());
        r.getOutputData().put("clicks", 480);
        r.getOutputData().put("impressions", 5000);
        r.getOutputData().put("conversionRate", 2.5);
        return r;
    }
}
