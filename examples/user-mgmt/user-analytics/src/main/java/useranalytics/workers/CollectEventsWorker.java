package useranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CollectEventsWorker implements Worker {
    @Override public String getTaskDefName() { return "uan_collect_events"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collected events for range: " + task.getInputData().get("dateRange"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("events", List.of("login", "page_view", "click"));
        r.getOutputData().put("totalEvents", 284500);
        return r;
    }
}
