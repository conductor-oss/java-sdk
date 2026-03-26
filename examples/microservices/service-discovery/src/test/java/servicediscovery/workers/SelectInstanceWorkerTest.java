package servicediscovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectInstanceWorkerTest {

    private final SelectInstanceWorker worker = new SelectInstanceWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_select_instance", worker.getTaskDefName());
    }

    @Test
    void selectsLeastConnections() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 12),
                Map.of("id", "inst-02", "host", "10.0.1.11", "port", 8080, "health", "healthy", "connections", 5),
                Map.of("id", "inst-03", "host", "10.0.1.12", "port", 8080, "health", "degraded", "connections", 30)
        );
        Task task = taskWith(Map.of("instances", instances, "strategy", "least-connections"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> selected = (Map<String, Object>) result.getOutputData().get("selectedInstance");
        assertEquals("inst-02", selected.get("id"));
    }

    @Test
    void filtersOutDegradedInstances() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "degraded", "connections", 1),
                Map.of("id", "inst-02", "host", "10.0.1.11", "port", 8080, "health", "healthy", "connections", 50)
        );
        Task task = taskWith(Map.of("instances", instances, "strategy", "least-connections"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> selected = (Map<String, Object>) result.getOutputData().get("selectedInstance");
        assertEquals("inst-02", selected.get("id"));
    }

    @Test
    void returnsStrategy() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 5)
        );
        Task task = taskWith(Map.of("instances", instances, "strategy", "least-connections"));
        TaskResult result = worker.execute(task);

        assertEquals("least-connections", result.getOutputData().get("strategy"));
    }

    @Test
    void handlesNullStrategy() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 5)
        );
        Map<String, Object> input = new HashMap<>();
        input.put("instances", instances);
        input.put("strategy", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("least-connections", result.getOutputData().get("strategy"));
    }

    @Test
    void handlesNullInstances() {
        Map<String, Object> input = new HashMap<>();
        input.put("instances", null);
        input.put("strategy", "least-connections");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesSingleInstance() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 10)
        );
        Task task = taskWith(Map.of("instances", instances, "strategy", "least-connections"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> selected = (Map<String, Object>) result.getOutputData().get("selectedInstance");
        assertEquals("inst-01", selected.get("id"));
    }

    @Test
    void outputContainsSelectedInstanceKey() {
        List<Map<String, Object>> instances = List.of(
                Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080, "health", "healthy", "connections", 5)
        );
        Task task = taskWith(Map.of("instances", instances, "strategy", "least-connections"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("selectedInstance"));
        assertTrue(result.getOutputData().containsKey("strategy"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
