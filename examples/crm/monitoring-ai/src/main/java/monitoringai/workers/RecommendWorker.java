package monitoringai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class RecommendWorker implements Worker {
    @Override public String getTaskDefName() { return "mai_recommend"; }
    @Override public TaskResult execute(Task task) {
        List<String> recommendations = List.of(
            "Increase connection pool size from 20 to 50",
            "Add connection pool monitoring alerts",
            "Enable query result caching"
        );
        System.out.println("  [recommend] " + recommendations.size() + " recommendations generated");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recommendations", recommendations);
        return result;
    }
}
