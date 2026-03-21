package eventwindowing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectWindowWorkerTest {

    private final CollectWindowWorker worker = new CollectWindowWorker();

    @Test
    void taskDefName() {
        assertEquals("ew_collect_window", worker.getTaskDefName());
    }

    @Test
    void collectsEventsIntoWindow() {
        List<Map<String, Object>> events = List.of(
                Map.of("ts", 1000, "value", 10),
                Map.of("ts", 1500, "value", 25),
                Map.of("ts", 2000, "value", 15),
                Map.of("ts", 2500, "value", 30),
                Map.of("ts", 3000, "value", 20));
        Task task = taskWith(Map.of("events", events, "windowSizeMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(events, result.getOutputData().get("windowEvents"));
    }

    @Test
    void returnsWindowId() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 1000, "value", 10));
        Task task = taskWith(Map.of("events", events, "windowSizeMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void returnsWindowSizeMs() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 1000, "value", 10));
        Task task = taskWith(Map.of("events", events, "windowSizeMs", 3000));
        TaskResult result = worker.execute(task);

        assertEquals(3000, result.getOutputData().get("windowSizeMs"));
    }

    @Test
    void handlesEmptyEventsList() {
        Task task = taskWith(Map.of("events", List.of(), "windowSizeMs", 5000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windowEvents =
                (List<Map<String, Object>>) result.getOutputData().get("windowEvents");
        assertTrue(windowEvents.isEmpty());
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("windowSizeMs", 5000);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windowEvents =
                (List<Map<String, Object>>) result.getOutputData().get("windowEvents");
        assertTrue(windowEvents.isEmpty());
    }

    @Test
    void handlesMissingWindowSize() {
        Task task = taskWith(Map.of("events", List.of(Map.of("ts", 1000, "value", 10))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5000, result.getOutputData().get("windowSizeMs"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("windowEvents"));
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
        assertEquals(5000, result.getOutputData().get("windowSizeMs"));
    }

    @Test
    void preservesAllEventFields() {
        List<Map<String, Object>> events = List.of(
                Map.of("ts", 1000, "value", 10),
                Map.of("ts", 1500, "value", 25),
                Map.of("ts", 2000, "value", 15),
                Map.of("ts", 2500, "value", 30),
                Map.of("ts", 3000, "value", 20));
        Task task = taskWith(Map.of("events", events, "windowSizeMs", 5000));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windowEvents =
                (List<Map<String, Object>>) result.getOutputData().get("windowEvents");
        assertEquals(5, windowEvents.size());
        assertEquals(1000, windowEvents.get(0).get("ts"));
        assertEquals(10, windowEvents.get(0).get("value"));
        assertEquals(3000, windowEvents.get(4).get("ts"));
        assertEquals(20, windowEvents.get(4).get("value"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
