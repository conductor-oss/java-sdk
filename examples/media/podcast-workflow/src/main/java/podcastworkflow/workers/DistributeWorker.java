package podcastworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "pod_distribute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [distribute] Processing " + task.getInputData().getOrDefault("directories", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("directories", List.of("Apple Podcasts"));
        r.getOutputData().put("pingsSent", 4);
        r.getOutputData().put("distributedAt", "2026-03-08T06:05:00Z");
        return r;
    }
}
