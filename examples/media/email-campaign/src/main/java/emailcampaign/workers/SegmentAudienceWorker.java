package emailcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SegmentAudienceWorker implements Worker {
    @Override public String getTaskDefName() { return "eml_segment_audience"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [segment] Processing " + task.getInputData().getOrDefault("segments", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("segments", List.of("active_users"));
        r.getOutputData().put("recipientCount", 15000);
        r.getOutputData().put("suppressedCount", 320);
        return r;
    }
}
