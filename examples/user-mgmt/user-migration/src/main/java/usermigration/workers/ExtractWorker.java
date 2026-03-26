package usermigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ExtractWorker implements Worker {
    @Override public String getTaskDefName() { return "umg_extract"; }
    @Override public TaskResult execute(Task task) {
        String sourceDb = (String) task.getInputData().get("sourceDb");
        System.out.println("  [extract] Extracted 500 users from " + sourceDb);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("users", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)));
        result.getOutputData().put("extractedCount", 500);
        return result;
    }
}
