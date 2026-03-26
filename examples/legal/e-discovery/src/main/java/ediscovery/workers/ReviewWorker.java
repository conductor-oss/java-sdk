package ediscovery.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "edc_review"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [edc_review] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", "reviewed");
        result.getOutputData().put("sources", Map.of("email",true,"slack",true));
        result.getOutputData().put("collected", Map.of("totalItems",45000,"sizeGB",120));
        result.getOutputData().put("processed", Map.of("unique",28000));
        result.getOutputData().put("results", Map.of("responsive",8500,"privileged",320));
        return result;
    }
}
