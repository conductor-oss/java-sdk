package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerApiWorkerTest {

    private final WorkerApiWorker worker = new WorkerApiWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_worker_api", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void producesEndpointsWithPathMethodsStatus() {
        Map<String, Object> apiTask = Map.of(
                "endpoints", List.of("/api/users", "/api/tasks"),
                "auth", "JWT",
                "validation", "schema-based"
        );
        Task task = taskWith(Map.of("task", apiTask));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> apiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) apiResult.get("endpoints");
        assertEquals(2, endpoints.size());

        Map<String, Object> first = endpoints.get(0);
        assertEquals("/api/users", first.get("path"));
        assertNotNull(first.get("methods"));
        assertEquals("implemented", first.get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resultContainsMiddlewareAndLinesOfCode() {
        Map<String, Object> apiTask = Map.of("endpoints", List.of("/api/data"));
        Task task = taskWith(Map.of("task", apiTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> apiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> middleware = (List<String>) apiResult.get("middleware");
        assertTrue(middleware.contains("auth"));
        assertTrue(middleware.contains("validation"));
        assertTrue(middleware.contains("errorHandler"));
        assertEquals(320, apiResult.get("linesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void emptyEndpointsProducesEmptyList() {
        Map<String, Object> apiTask = Map.of("endpoints", List.of());
        Task task = taskWith(Map.of("task", apiTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> apiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<?> endpoints = (List<?>) apiResult.get("endpoints");
        assertTrue(endpoints.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullTask() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> apiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<?> endpoints = (List<?>) apiResult.get("endpoints");
        assertTrue(endpoints.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
