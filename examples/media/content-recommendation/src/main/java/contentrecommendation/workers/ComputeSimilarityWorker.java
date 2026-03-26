package contentrecommendation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ComputeSimilarityWorker implements Worker {
    @Override public String getTaskDefName() { return "crm_compute_similarity"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [similarity] Processing " + task.getInputData().getOrDefault("similarItems", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("similarItems", List.of());
        r.getOutputData().put("id", "VID-550");
        r.getOutputData().put("score", 0.94);
        r.getOutputData().put("category", "technology");
        return r;
    }
}
