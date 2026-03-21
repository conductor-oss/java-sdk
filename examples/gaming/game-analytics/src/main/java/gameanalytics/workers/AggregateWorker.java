package gameanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "gan_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Aggregating metrics");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("aggregated", Map.of("dau", 3200, "avgSessionMin", 42, "retention7d", 0.45));
        return r;
    }
}
