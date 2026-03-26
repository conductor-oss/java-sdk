package gameanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class CollectEventsWorker implements Worker {
    @Override public String getTaskDefName() { return "gan_collect_events"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Collecting events for " + task.getInputData().get("gameId") + ", range: " + task.getInputData().get("dateRange"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("events", 125000); r.addOutputData("types", List.of("login","match","purchase","achievement"));
        return r;
    }
}
