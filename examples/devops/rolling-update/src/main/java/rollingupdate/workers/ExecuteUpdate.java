package rollingupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the rolling update according to the plan.
 */
public class ExecuteUpdate implements Worker {

    @Override
    public String getTaskDefName() {
        return "ru_execute";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> executeData = (Map<String, Object>) task.getInputData().get("executeData");

        int totalBatches = 3;
        if (executeData != null && executeData.get("totalBatches") != null) {
            totalBatches = ((Number) executeData.get("totalBatches")).intValue();
        }

        System.out.println("[ru_execute] Executing rolling update across " + totalBatches + " batches");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("execute", true);
        output.put("processed", true);
        output.put("batchesCompleted", totalBatches);
        output.put("totalBatches", totalBatches);
        output.put("rollbackTriggered", false);
        output.put("updatedAt", Instant.now().toString());

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
