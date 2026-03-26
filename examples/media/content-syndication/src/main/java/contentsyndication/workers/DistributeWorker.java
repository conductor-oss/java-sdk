package contentsyndication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "syn_distribute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [distribute] Processing " + task.getInputData().getOrDefault("distributedPlatforms", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("distributedPlatforms", "versions.map((v) => v.platform)");
        r.getOutputData().put("distributedCount", "versions.length");
        r.getOutputData().put("urls", Map.of());
        r.getOutputData().put("medium", "https://medium.com/@example/workflow-orchestration-523");
        r.getOutputData().put("devto", "https://dev.to/example/workflow-orchestration-523");
        r.getOutputData().put("hashnode", "https://hashnode.com/post/workflow-orchestration-523");
        return r;
    }
}
