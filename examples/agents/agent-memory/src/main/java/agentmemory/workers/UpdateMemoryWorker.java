package agentmemory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Updates the memory store with the latest interaction data.
 * Returns a memorySnapshot and memorySize.
 */
public class UpdateMemoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_update_memory";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");

        if (userId == null || userId.isBlank()) {
            userId = "unknown";
        }

        System.out.println("  [am_update_memory] Updating memory for user: " + userId);

        Map<String, Object> memorySnapshot = new LinkedHashMap<>();
        memorySnapshot.put("totalEntries", 6);
        memorySnapshot.put("factsStored", 5);
        memorySnapshot.put("lastUpdated", "2025-01-15T10:00:00Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("memorySnapshot", memorySnapshot);
        result.getOutputData().put("memorySize", 6);
        return result;
    }
}
