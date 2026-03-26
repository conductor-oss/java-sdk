package eventtransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapSchemaWorkerTest {

    private final MapSchemaWorker worker = new MapSchemaWorker();

    @Test
    void taskDefName() {
        assertEquals("et_map_schema", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapsToCloudEventsFormat() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of(
                        "id", "evt-1",
                        "type", "repository.push",
                        "timestamp", "2026-03-08T10:00:00Z",
                        "actor", Map.of("id", "U-5501", "name", "jdoe"),
                        "resource", Map.of("type", "repository", "id", "repo-1"),
                        "action", "push",
                        "enrichment", Map.of("geoLocation", "us-west-2")),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertEquals("1.0", mapped.get("specversion"));
        assertEquals("com.example.repository.push", mapped.get("type"));
        assertEquals("/users/U-5501", mapped.get("source"));
        assertEquals("evt-1", mapped.get("id"));
        assertEquals("2026-03-08T10:00:00Z", mapped.get("time"));
        assertEquals("application/json", mapped.get("datacontenttype"));
        assertEquals("repository/repo-1", mapped.get("subject"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapsDataPayload() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of(
                        "id", "evt-1",
                        "type", "test.event",
                        "timestamp", "2026-01-01T00:00:00Z",
                        "actor", Map.of("id", "u1", "name", "user"),
                        "resource", Map.of("type", "repo", "id", "r1"),
                        "action", "read",
                        "enrichment", Map.of("riskScore", 0.12)),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        Map<String, Object> data = (Map<String, Object>) mapped.get("data");
        assertNotNull(data);
        assertNotNull(data.get("actor"));
        assertNotNull(data.get("resource"));
        assertEquals("read", data.get("action"));
        assertNotNull(data.get("enrichment"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void setsCorrectSubjectFormat() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of(
                        "resource", Map.of("type", "repository", "id", "repo-conductor-workflows")),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertEquals("repository/repo-conductor-workflows", mapped.get("subject"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void setsCorrectSourceFormat() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of(
                        "actor", Map.of("id", "U-5501")),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertEquals("/users/U-5501", mapped.get("source"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void prefixesTypeWithComExample() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of("type", "user.login"),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertEquals("com.example.user.login", mapped.get("type"));
    }

    @Test
    void handlesEmptyEnrichedEvent() {
        Task task = taskWith(Map.of(
                "enrichedEvent", Map.of(),
                "targetFormat", "cloudevents"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("mappedEvent"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesMissingEnrichedEvent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertEquals("1.0", mapped.get("specversion"));
        assertEquals("com.example.unknown", mapped.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesMissingTargetFormat() {
        Map<String, Object> input = new HashMap<>();
        input.put("enrichedEvent", Map.of("id", "evt-1", "type", "test"));
        input.put("targetFormat", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> mapped = (Map<String, Object>) result.getOutputData().get("mappedEvent");
        assertNotNull(mapped);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
