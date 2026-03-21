package eventtransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps an enriched event to CloudEvents format (specversion 1.0).
 * Produces: specversion, type, source, id, time, datacontenttype, subject, and data.
 */
public class MapSchemaWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_map_schema";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> enrichedEvent = (Map<String, Object>) task.getInputData().get("enrichedEvent");
        if (enrichedEvent == null) {
            enrichedEvent = Map.of();
        }
        String targetFormat = (String) task.getInputData().get("targetFormat");
        if (targetFormat == null || targetFormat.isBlank()) {
            targetFormat = "cloudevents";
        }

        System.out.println("  [et_map_schema] Mapping event to format: " + targetFormat);

        String eventType = (String) enrichedEvent.getOrDefault("type", "unknown");
        String eventId = (String) enrichedEvent.getOrDefault("id", "unknown");
        String timestamp = (String) enrichedEvent.getOrDefault("timestamp", "1970-01-01T00:00:00Z");

        Map<String, Object> actor = (Map<String, Object>) enrichedEvent.getOrDefault("actor", Map.of());
        Map<String, Object> resource = (Map<String, Object>) enrichedEvent.getOrDefault("resource", Map.of());
        String action = (String) enrichedEvent.getOrDefault("action", "unknown");
        Object enrichment = enrichedEvent.getOrDefault("enrichment", Map.of());

        String actorId = (String) actor.getOrDefault("id", "unknown");
        String resourceType = (String) resource.getOrDefault("type", "unknown");
        String resourceId = (String) resource.getOrDefault("id", "unknown");

        // Build CloudEvents envelope
        Map<String, Object> mappedEvent = new LinkedHashMap<>();
        mappedEvent.put("specversion", "1.0");
        mappedEvent.put("type", "com.example." + eventType);
        mappedEvent.put("source", "/users/" + actorId);
        mappedEvent.put("id", eventId);
        mappedEvent.put("time", timestamp);
        mappedEvent.put("datacontenttype", "application/json");
        mappedEvent.put("subject", resourceType + "/" + resourceId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actor", actor);
        data.put("resource", resource);
        data.put("action", action);
        data.put("enrichment", enrichment);
        mappedEvent.put("data", data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mappedEvent", mappedEvent);
        return result;
    }
}
