package contentmoderation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitContentWorker implements Worker {
    @Override public String getTaskDefName() { return "mod_submit_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Processing " + task.getInputData().getOrDefault("receivedAt", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receivedAt", "2026-03-08T10:00:00Z");
        r.getOutputData().put("queuePosition", 1);
        return r;
    }
}
