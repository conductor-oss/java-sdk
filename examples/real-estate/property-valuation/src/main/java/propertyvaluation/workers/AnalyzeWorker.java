package propertyvaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "pvl_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Average comp price analyzed");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", Map.of("avgPrice",465000,"pricePerSqft",215,"trend","stable"));
        return result;
    }
}
