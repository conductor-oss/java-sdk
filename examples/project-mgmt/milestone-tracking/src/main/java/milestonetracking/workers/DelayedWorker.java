package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DelayedWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_delayed"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [delayed] Milestone " + task.getInputData().get("milestoneId") + " is delayed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "reschedule"); return r;
    }
}
