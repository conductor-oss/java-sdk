package incrementalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that detects changed documents in the source collection since
 * the last sync timestamp. Returns changed document IDs and their
 * existing content hashes (null for new documents).
 */
public class DetectChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ir_detect_changes";
    }

    @Override
    public TaskResult execute(Task task) {
        String sourceCollection = (String) task.getInputData().get("sourceCollection");
        String lastSyncTimestamp = (String) task.getInputData().get("lastSyncTimestamp");

        List<String> changedDocIds = List.of("doc-101", "doc-205", "doc-307", "doc-412");

        Map<String, String> existingHashes = Map.of(
                "doc-101", "a1b2c3",
                "doc-307", "d4e5f6"
        );

        // Use a HashMap so null values are supported for new docs
        java.util.Map<String, String> hashesWithNulls = new java.util.HashMap<>();
        hashesWithNulls.put("doc-101", "a1b2c3");
        hashesWithNulls.put("doc-205", null);
        hashesWithNulls.put("doc-307", "d4e5f6");
        hashesWithNulls.put("doc-412", null);

        System.out.println("  [detect_changes] Source: " + sourceCollection
                + ", since: " + lastSyncTimestamp
                + ", changed: " + changedDocIds.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("changedDocIds", changedDocIds);
        result.getOutputData().put("existingHashes", hashesWithNulls);
        result.getOutputData().put("totalChanged", 4);
        return result;
    }
}
