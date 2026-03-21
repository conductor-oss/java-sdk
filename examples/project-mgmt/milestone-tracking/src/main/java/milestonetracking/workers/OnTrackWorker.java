package milestonetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class OnTrackWorker implements Worker {
    @Override public String getTaskDefName() { return "mst_on_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [on_track] Milestone " + task.getInputData().get("milestoneId") + " is on track");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "continue"); return r;
    }
}
