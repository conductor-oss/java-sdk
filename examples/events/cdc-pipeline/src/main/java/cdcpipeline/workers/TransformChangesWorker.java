package cdcpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms raw CDC change records into structured event payloads with
 * eventType, entityId, payload, previousPayload, and metadata.
 */
public class TransformChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cd_transform_changes";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> changes = (List<Map<String, Object>>) task.getInputData().get("changes");
        if (changes == null) {
            changes = List.of();
        }

        String sourceTable = (String) task.getInputData().get("sourceTable");
        if (sourceTable == null || sourceTable.isBlank()) {
            sourceTable = "users";
        }

        System.out.println("  [cd_transform_changes] Transforming " + changes.size() + " changes");

        List<Map<String, Object>> transformedChanges = new ArrayList<>();
        for (Map<String, Object> change : changes) {
            String operation = (String) change.get("operation");
            Map<String, Object> data = (Map<String, Object>) change.get("data");
            Map<String, Object> previousData = (Map<String, Object>) change.get("previousData");
            String timestamp = (String) change.get("timestamp");

            Object entityId = data != null ? data.get("id") : null;
            String eventType = sourceTable + "." + (operation != null ? operation.toLowerCase() : "unknown");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", sourceTable);
            metadata.put("operation", operation);
            metadata.put("capturedAt", timestamp);

            Map<String, Object> transformed = new HashMap<>();
            transformed.put("eventType", eventType);
            transformed.put("entityId", entityId);
            transformed.put("payload", data);
            transformed.put("previousPayload", previousData);
            transformed.put("metadata", metadata);

            transformedChanges.add(transformed);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformedChanges", transformedChanges);
        return result;
    }
}
