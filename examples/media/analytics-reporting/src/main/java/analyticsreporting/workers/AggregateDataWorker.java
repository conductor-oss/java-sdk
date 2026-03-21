package analyticsreporting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class AggregateDataWorker implements Worker {
    @Override public String getTaskDefName() { return "anr_aggregate_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Processing " + task.getInputData().getOrDefault("aggregatedData", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("aggregatedData", Map.of());
        r.getOutputData().put("totalSessions", 85000);
        r.getOutputData().put("uniqueUsers", 42000);
        r.getOutputData().put("pageViews", 320000);
        r.getOutputData().put("avgSessionDuration", 245);
        return r;
    }
}
