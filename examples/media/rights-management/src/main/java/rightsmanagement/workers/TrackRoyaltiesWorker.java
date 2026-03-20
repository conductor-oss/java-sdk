package rightsmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackRoyaltiesWorker implements Worker {
    @Override public String getTaskDefName() { return "rts_track_royalties"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [royalties] Processing " + task.getInputData().getOrDefault("paymentDue", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("paymentDue", "2026-04-01");
        r.getOutputData().put("rightsHolder", "Music Rights Corp");
        return r;
    }
}
