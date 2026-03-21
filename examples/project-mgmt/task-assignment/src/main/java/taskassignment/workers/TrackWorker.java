package taskassignment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "tas_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking task assigned to " + task.getInputData().get("assignee"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("tracking", Map.of("assignee",String.valueOf(task.getInputData().get("assignee")),"status","IN_PROGRESS","dueDate","2026-03-15")); return r;
    }
}
