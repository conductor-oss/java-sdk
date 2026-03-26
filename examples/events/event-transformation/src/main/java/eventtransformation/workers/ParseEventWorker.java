package eventtransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses a raw event from a legacy format into a normalized structure.
 * Extracts and renames fields: event_id->id, event_type->type, ts->timestamp,
 * user_id/user_name->actor, resource_type/resource_id->resource, action, metadata->details.
 */
public class ParseEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_parse_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> rawEvent = (Map<String, Object>) task.getInputData().get("rawEvent");
        if (rawEvent == null) {
            rawEvent = Map.of();
        }
        String sourceFormat = (String) task.getInputData().get("sourceFormat");
        if (sourceFormat == null || sourceFormat.isBlank()) {
            sourceFormat = "unknown";
        }

        System.out.println("  [et_parse_event] Parsing raw event from format: " + sourceFormat);

        // Extract and rename fields from the raw event
        String id = (String) rawEvent.getOrDefault("event_id", "unknown");
        String type = (String) rawEvent.getOrDefault("event_type", "unknown");
        String timestamp = (String) rawEvent.getOrDefault("ts", "1970-01-01T00:00:00Z");

        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("id", rawEvent.getOrDefault("user_id", "unknown"));
        actor.put("name", rawEvent.getOrDefault("user_name", "unknown"));

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("type", rawEvent.getOrDefault("resource_type", "unknown"));
        resource.put("id", rawEvent.getOrDefault("resource_id", "unknown"));

        String action = (String) rawEvent.getOrDefault("action", "unknown");

        Object details = rawEvent.getOrDefault("metadata", Map.of());

        Map<String, Object> parsedEvent = new LinkedHashMap<>();
        parsedEvent.put("id", id);
        parsedEvent.put("type", type);
        parsedEvent.put("timestamp", timestamp);
        parsedEvent.put("actor", actor);
        parsedEvent.put("resource", resource);
        parsedEvent.put("action", action);
        parsedEvent.put("details", details);

        int fieldCount = 7;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsedEvent", parsedEvent);
        result.getOutputData().put("eventType", type);
        result.getOutputData().put("fieldCount", fieldCount);
        return result;
    }
}
