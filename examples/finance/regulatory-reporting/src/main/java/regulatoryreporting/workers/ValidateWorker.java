package regulatoryreporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "reg_validate"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("data");
        if (data == null) data = Map.of();
        int errors = 0;
        if (data.get("totalAssets") == null) errors++;
        if (data.get("capitalRatio") == null) errors++;
        System.out.println("  [validate] Validation: " + errors + " errors found");
        result.getOutputData().put("validatedData", data);
        result.getOutputData().put("errorCount", errors);
        result.getOutputData().put("passed", errors == 0);
        return result;
    }
}
