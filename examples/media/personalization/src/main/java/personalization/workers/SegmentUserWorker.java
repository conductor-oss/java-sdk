package personalization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SegmentUserWorker implements Worker {
    @Override public String getTaskDefName() { return "per_segment_user"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [segment] Processing " + task.getInputData().getOrDefault("segment", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("segment", "tech_professional");
        r.getOutputData().put("subSegments", List.of("cloud_enthusiast"));
        r.getOutputData().put("confidence", 0.88);
        return r;
    }
}
