package liveops.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CloseWorker implements Worker {
    @Override public String getTaskDefName() { return "lop_close"; }
    @Override public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        System.out.println("  [close] Closing event " + eventId);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("event", Map.of("eventId", eventId != null ? eventId : "EVT-748", "participants", 15000, "rewards_distributed", 12800, "status", "CLOSED"));
        return r;
    }
}
