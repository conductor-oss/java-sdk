package useranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "uan_aggregate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Events aggregated by segment");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("aggregated", Map.of("byDay", Map.of(), "bySegment", Map.of()));
        r.getOutputData().put("uniqueUsers", 12340);
        return r;
    }
}
