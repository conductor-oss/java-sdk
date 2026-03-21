package eventtransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseEventWorkerTest {

    private final ParseEventWorker worker = new ParseEventWorker();

    @Test
    void taskDefName() {
        assertEquals("et_parse_event", worker.getTaskDefName());
    }

    @Test
    void parsesStandardRawEvent() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "event_id", "raw-evt-fixed-001",
                        "event_type", "repository.push",
                        "ts", "2026-03-08T10:00:00Z",
                        "user_id", "U-5501",
                        "user_name", "jdoe",
                        "resource_type", "repository",
                        "resource_id", "repo-conductor-workflows",
                        "action", "push",
                        "metadata", Map.of("branch", "main", "commits", 3)),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("repository.push", result.getOutputData().get("eventType"));
        assertEquals(7, result.getOutputData().get("fieldCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsIdAndType() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "event_id", "evt-123",
                        "event_type", "user.login",
                        "ts", "2026-01-01T00:00:00Z"),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        assertEquals("evt-123", parsed.get("id"));
        assertEquals("user.login", parsed.get("type"));
        assertEquals("2026-01-01T00:00:00Z", parsed.get("timestamp"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsActorFields() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "user_id", "U-5501",
                        "user_name", "jdoe"),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        Map<String, Object> actor = (Map<String, Object>) parsed.get("actor");
        assertEquals("U-5501", actor.get("id"));
        assertEquals("jdoe", actor.get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsResourceFields() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "resource_type", "repository",
                        "resource_id", "repo-conductor-workflows"),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        Map<String, Object> resource = (Map<String, Object>) parsed.get("resource");
        assertEquals("repository", resource.get("type"));
        assertEquals("repo-conductor-workflows", resource.get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void extractsActionAndDetails() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "action", "push",
                        "metadata", Map.of("branch", "main", "commits", 3)),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        assertEquals("push", parsed.get("action"));
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) parsed.get("details");
        assertEquals("main", details.get("branch"));
        assertEquals(3, details.get("commits"));
    }

    @Test
    void handlesEmptyRawEvent() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("parsedEvent"));
        assertEquals("unknown", result.getOutputData().get("eventType"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesMissingRawEvent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        assertNotNull(parsed);
        assertEquals("unknown", parsed.get("id"));
        assertEquals("unknown", parsed.get("type"));
    }

    @Test
    void handlesMissingSourceFormat() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawEvent", Map.of("event_id", "evt-1"));
        input.put("sourceFormat", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parsedEventHasSevenFields() {
        Task task = taskWith(Map.of(
                "rawEvent", Map.of(
                        "event_id", "evt-1",
                        "event_type", "test",
                        "ts", "2026-01-01T00:00:00Z",
                        "user_id", "u1",
                        "user_name", "user",
                        "resource_type", "repo",
                        "resource_id", "r1",
                        "action", "read",
                        "metadata", Map.of()),
                "sourceFormat", "legacy_v1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsed = (Map<String, Object>) result.getOutputData().get("parsedEvent");
        assertEquals(7, parsed.size());
        assertTrue(parsed.containsKey("id"));
        assertTrue(parsed.containsKey("type"));
        assertTrue(parsed.containsKey("timestamp"));
        assertTrue(parsed.containsKey("actor"));
        assertTrue(parsed.containsKey("resource"));
        assertTrue(parsed.containsKey("action"));
        assertTrue(parsed.containsKey("details"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
