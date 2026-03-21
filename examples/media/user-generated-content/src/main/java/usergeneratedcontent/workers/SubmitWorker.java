package usergeneratedcontent.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "ugc_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Processing " + task.getInputData().getOrDefault("receivedAt", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("receivedAt", "2026-03-08T09:00:00Z");
        r.getOutputData().put("queuePosition", 3);
        return r;
    }
}
