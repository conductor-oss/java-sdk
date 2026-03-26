package legalcasemanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "lcm_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [lcm_track] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("phase", "resolution");
        return result;
    }
}
