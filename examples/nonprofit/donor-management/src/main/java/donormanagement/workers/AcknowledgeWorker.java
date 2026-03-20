package donormanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AcknowledgeWorker implements Worker {
    @Override public String getTaskDefName() { return "dnr_acknowledge"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [acknowledge] Acknowledgment sent to " + task.getInputData().get("donorName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("acknowledged", true); r.addOutputData("method", "personal-letter"); return r;
    }
}
