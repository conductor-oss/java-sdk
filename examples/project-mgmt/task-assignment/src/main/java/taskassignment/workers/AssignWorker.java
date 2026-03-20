package taskassignment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssignWorker implements Worker {
    @Override public String getTaskDefName() { return "tas_assign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assign] Assigning task to candidate");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("assignee", "Alice"); r.getOutputData().put("assigned", true); return r;
    }
}
