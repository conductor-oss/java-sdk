package liveops.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ConfigureWorker implements Worker {
    @Override public String getTaskDefName() { return "lop_configure"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [config] Configuring event " + task.getInputData().get("eventId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("config", Map.of("rewards", List.of("Double XP","Rare Skin"), "difficulty", "hard", "duration", "72h"));
        return r;
    }
}
