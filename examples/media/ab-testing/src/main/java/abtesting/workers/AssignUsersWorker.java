package abtesting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssignUsersWorker implements Worker {
    @Override public String getTaskDefName() { return "abt_assign_users"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assign] Processing " + task.getInputData().getOrDefault("groupASize", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("groupASize", "halfSize");
        r.getOutputData().put("groupBSize", "size - halfSize");
        r.getOutputData().put("assignmentMethod", "random_hash");
        return r;
    }
}
