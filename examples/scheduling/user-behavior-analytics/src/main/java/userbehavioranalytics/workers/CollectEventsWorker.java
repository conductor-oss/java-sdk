package userbehavioranalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectEventsWorker implements Worker {
    @Override public String getTaskDefName() { return "uba_collect_events"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting events for user " + task.getInputData().get("userId"));
        r.getOutputData().put("eventCount", 342);
        r.getOutputData().put("eventTypes", List.of("login","page_view","api_call","download"));
        return r;
    }
}
