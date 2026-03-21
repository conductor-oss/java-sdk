package hierarchicalagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerUiWorkerTest {

    private final WorkerUiWorker worker = new WorkerUiWorker();

    @Test
    void taskDefName() {
        assertEquals("hier_worker_ui", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void producesComponentsWithNameAndStatus() {
        Map<String, Object> uiTask = Map.of(
                "pages", List.of("Dashboard", "Settings"),
                "framework", "React",
                "stateManagement", "Redux"
        );
        Task task = taskWith(Map.of("task", uiTask));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> uiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<Map<String, Object>> components = (List<Map<String, Object>>) uiResult.get("components");
        assertEquals(2, components.size());
        assertEquals("Dashboard", components.get(0).get("name"));
        assertEquals("implemented", components.get(0).get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resultContainsSharedComponentsAndLinesOfCode() {
        Map<String, Object> uiTask = Map.of("pages", List.of("Home"));
        Task task = taskWith(Map.of("task", uiTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> uiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> shared = (List<String>) uiResult.get("sharedComponents");
        assertTrue(shared.contains("Header"));
        assertTrue(shared.contains("Footer"));
        assertTrue(shared.contains("Sidebar"));
        assertTrue(shared.contains("Loading"));
        assertEquals(450, uiResult.get("linesOfCode"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void emptyPagesProducesEmptyComponents() {
        Map<String, Object> uiTask = Map.of("pages", List.of());
        Task task = taskWith(Map.of("task", uiTask));
        TaskResult result = worker.execute(task);

        Map<String, Object> uiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<?> components = (List<?>) uiResult.get("components");
        assertTrue(components.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullTask() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> uiResult = (Map<String, Object>) result.getOutputData().get("result");
        List<?> components = (List<?>) uiResult.get("components");
        assertTrue(components.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
