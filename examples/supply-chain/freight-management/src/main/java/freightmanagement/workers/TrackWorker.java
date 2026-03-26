package freightmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "frm_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] " + task.getInputData().get("bookingId") + ": in transit — 65% complete");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("status", "in_transit"); r.getOutputData().put("progress", 65); return r;
    }
}
