package itineraryplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class PreferencesWorker implements Worker {
    @Override public String getTaskDefName() { return "itp_preferences"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [preferences] Loaded preferences for " + task.getInputData().get("travelerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("preferences", Map.of("airline","Delta","hotel","4-star","seatClass","economy-plus"));
        return r;
    }
}
