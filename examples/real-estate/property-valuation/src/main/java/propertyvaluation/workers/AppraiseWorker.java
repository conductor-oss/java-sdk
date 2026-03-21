package propertyvaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AppraiseWorker implements Worker {
    @Override public String getTaskDefName() { return "pvl_appraise"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [appraise] Appraised value: $465000");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("appraisal", Map.of("estimatedValue",465000,"confidence","high","method","sales-comparison"));
        return result;
    }
}
