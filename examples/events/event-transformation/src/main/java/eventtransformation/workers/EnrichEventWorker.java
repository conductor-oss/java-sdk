package eventtransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches a parsed event with additional context: department, role, and location
 * for the actor; project and environment for the resource; and an enrichment block
 * with geoLocation, riskScore, complianceFlags, and enrichedAt.
 */
public class EnrichEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "et_enrich_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> parsedEvent = (Map<String, Object>) task.getInputData().get("parsedEvent");
        if (parsedEvent == null) {
            parsedEvent = Map.of();
        }
        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "unknown";
        }

        System.out.println("  [et_enrich_event] Enriching event of type: " + eventType);

        // Deep copy the parsed event to avoid mutating the original
        Map<String, Object> enrichedEvent = new LinkedHashMap<>(parsedEvent);

        // Enrich actor with department, role, location
        Map<String, Object> actor = new LinkedHashMap<>();
        Object existingActor = parsedEvent.get("actor");
        if (existingActor instanceof Map) {
            actor.putAll((Map<String, Object>) existingActor);
        }
        actor.put("department", "Engineering");
        actor.put("role", "Senior Developer");
        actor.put("location", "US-West");
        enrichedEvent.put("actor", actor);

        // Enrich resource with project and environment
        Map<String, Object> resource = new LinkedHashMap<>();
        Object existingResource = parsedEvent.get("resource");
        if (existingResource instanceof Map) {
            resource.putAll((Map<String, Object>) existingResource);
        }
        resource.put("project", "conductor-platform");
        resource.put("environment", "production");
        enrichedEvent.put("resource", resource);

        // Add enrichment block
        Map<String, Object> enrichment = new LinkedHashMap<>();
        enrichment.put("geoLocation", "us-west-2");
        enrichment.put("riskScore", 0.12);
        enrichment.put("complianceFlags", List.of());
        enrichment.put("enrichedAt", "2026-01-15T10:00:00Z");
        enrichedEvent.put("enrichment", enrichment);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrichedEvent", enrichedEvent);
        result.getOutputData().put("fieldsAdded", 6);
        return result;
    }
}
