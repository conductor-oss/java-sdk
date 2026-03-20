package taskinputtemplates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildContextWorkerTest {

    private final BuildContextWorker worker = new BuildContextWorker();

    @Test
    void taskDefName() {
        assertEquals("tpl_build_context", worker.getTaskDefName());
    }

    @Test
    void adminGetsFullPermissions() {
        Task task = taskWith(Map.of(
                "requestContext", new HashMap<>(Map.of(
                        "user", Map.of("name", "Alice", "role", "admin"),
                        "action", "write",
                        "metadata", Map.of("source", "dashboard"),
                        "environment", "production",
                        "apiVersion", "v2",
                        "timestamp", "2024-01-01T00:00:00Z"
                ))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedContext");
        assertNotNull(enriched);
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) enriched.get("permissions");
        assertEquals(List.of("read", "write", "delete"), permissions);
    }

    @Test
    void viewerGetsReadOnlyPermissions() {
        Task task = taskWith(Map.of(
                "requestContext", new HashMap<>(Map.of(
                        "user", Map.of("name", "Bob", "role", "viewer"),
                        "action", "write",
                        "metadata", Map.of("source", "api"),
                        "environment", "production",
                        "apiVersion", "v2",
                        "timestamp", "2024-01-01T00:00:00Z"
                ))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedContext");
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) enriched.get("permissions");
        assertEquals(List.of("read"), permissions);
    }

    @Test
    void enrichedContextPreservesOriginalFields() {
        Task task = taskWith(Map.of(
                "requestContext", new HashMap<>(Map.of(
                        "user", Map.of("name", "Alice", "role", "admin"),
                        "action", "delete",
                        "metadata", Map.of("source", "cli"),
                        "environment", "staging",
                        "apiVersion", "v2",
                        "timestamp", "2024-06-15T12:00:00Z"
                ))
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedContext");
        assertEquals("delete", enriched.get("action"));
        assertEquals("staging", enriched.get("environment"));
        assertEquals("v2", enriched.get("apiVersion"));
        assertEquals("2024-06-15T12:00:00Z", enriched.get("timestamp"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
