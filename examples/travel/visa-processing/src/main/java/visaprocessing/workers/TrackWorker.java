package visaprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "vsp_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Application " + task.getInputData().get("applicationId") + ": under review");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("phase", "approved"); r.getOutputData().put("estimatedDate", "2024-04-01"); return r;
    }
}
