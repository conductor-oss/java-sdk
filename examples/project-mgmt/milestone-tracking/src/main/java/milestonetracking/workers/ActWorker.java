package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_act"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [act] Taking action for milestone " + task.getInputData().get("milestoneId") + ": status=" + task.getInputData().get("status"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "handled_" + task.getInputData().get("status")); return r;
    }
}
