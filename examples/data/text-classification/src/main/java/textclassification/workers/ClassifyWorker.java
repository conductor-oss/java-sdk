package textclassification.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "txc_classify"; }
    @Override public TaskResult execute(Task task) {
        Map<String, Object> scores = Map.of("technology", 0.87, "science", 0.72, "business", 0.15, "sports", 0.03);
        System.out.println("  [classify] Predicted category: technology");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prediction", "technology");
        result.getOutputData().put("scores", scores);
        return result;
    }
}
