package eventtransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichEventWorkerTest {

    private final EnrichEventWorker worker = new EnrichEventWorker();

    @Test
    void taskDefName() {
        assertEquals("et_enrich_event", worker.getTaskDefName());
    }

    @Test
    void enrichesStandardEvent() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "id", "evt-1",
                        "type", "repository.push",
                        "actor", Map.of("id", "U-5501", "name", "jdoe"),
                        "resource", Map.of("type", "repository", "id", "repo-1")),
                "eventType", "repository.push"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("fieldsAdded"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsActorEnrichment() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "actor", Map.of("id", "U-5501", "name", "jdoe"),
                        "resource", Map.of("type", "repo", "id", "r1")),
                "eventType", "repository.push"));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        Map<String, Object> actor = (Map<String, Object>) enriched.get("actor");
        assertEquals("Engineering", actor.get("department"));
        assertEquals("Senior Developer", actor.get("role"));
        assertEquals("US-West", actor.get("location"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsResourceEnrichment() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "actor", Map.of("id", "U-5501", "name", "jdoe"),
                        "resource", Map.of("type", "repository", "id", "repo-1")),
                "eventType", "repository.push"));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        Map<String, Object> resource = (Map<String, Object>) enriched.get("resource");
        assertEquals("conductor-platform", resource.get("project"));
        assertEquals("production", resource.get("environment"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsEnrichmentBlock() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "actor", Map.of("id", "U-5501"),
                        "resource", Map.of("type", "repo", "id", "r1")),
                "eventType", "repository.push"));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        Map<String, Object> enrichment = (Map<String, Object>) enriched.get("enrichment");
        assertNotNull(enrichment);
        assertEquals("us-west-2", enrichment.get("geoLocation"));
        assertEquals(0.12, enrichment.get("riskScore"));
        assertEquals(List.of(), enrichment.get("complianceFlags"));
        assertEquals("2026-01-15T10:00:00Z", enrichment.get("enrichedAt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalActorFields() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "actor", Map.of("id", "U-5501", "name", "jdoe"),
                        "resource", Map.of("type", "repo", "id", "r1")),
                "eventType", "test"));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        Map<String, Object> actor = (Map<String, Object>) enriched.get("actor");
        assertEquals("U-5501", actor.get("id"));
        assertEquals("jdoe", actor.get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalResourceFields() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(
                        "actor", Map.of("id", "U-5501"),
                        "resource", Map.of("type", "repository", "id", "repo-1")),
                "eventType", "test"));
        TaskResult result = worker.execute(task);

        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        Map<String, Object> resource = (Map<String, Object>) enriched.get("resource");
        assertEquals("repository", resource.get("type"));
        assertEquals("repo-1", resource.get("id"));
    }

    @Test
    void handlesEmptyParsedEvent() {
        Task task = taskWith(Map.of(
                "parsedEvent", Map.of(),
                "eventType", "unknown"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedEvent"));
        assertEquals(6, result.getOutputData().get("fieldsAdded"));
    }

    @Test
    void handlesMissingParsedEvent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedEvent"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesMissingEventType() {
        Map<String, Object> input = new HashMap<>();
        input.put("parsedEvent", Map.of("actor", Map.of("id", "u1"), "resource", Map.of("type", "r", "id", "1")));
        input.put("eventType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedEvent");
        assertNotNull(enriched.get("enrichment"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
