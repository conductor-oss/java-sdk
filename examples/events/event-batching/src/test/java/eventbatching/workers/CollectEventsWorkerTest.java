package eventbatching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectEventsWorkerTest {

    private final CollectEventsWorker worker = new CollectEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("eb_collect_events", worker.getTaskDefName());
    }

    @Test
    void collectsSixEvents() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("id", "e1", "type", "click"),
                Map.of("id", "e2", "type", "view"),
                Map.of("id", "e3", "type", "click"),
                Map.of("id", "e4", "type", "purchase"),
                Map.of("id", "e5", "type", "view"),
                Map.of("id", "e6", "type", "click"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("totalCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsEventsUnchanged() {
        List<Map<String, Object>> events = List.of(
                Map.of("id", "e1", "type", "click"),
                Map.of("id", "e2", "type", "view"));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputEvents =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertEquals(2, outputEvents.size());
        assertEquals("e1", outputEvents.get(0).get("id"));
        assertEquals("e2", outputEvents.get(1).get("id"));
    }

    @Test
    void countMatchesEventListSize() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("id", "e1", "type", "click"),
                Map.of("id", "e2", "type", "view"),
                Map.of("id", "e3", "type", "click"))));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesSingleEvent() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("id", "e1", "type", "click"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesEmptyEventList() {
        Task task = taskWith(Map.of("events", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
        List<Map<String, Object>> outputEvents =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertTrue(outputEvents.isEmpty());
    }

    @Test
    void handlesMissingEventsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesEventTypes() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("id", "e1", "type", "click"),
                Map.of("id", "e4", "type", "purchase"))));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputEvents =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertEquals("click", outputEvents.get(0).get("type"));
        assertEquals("purchase", outputEvents.get(1).get("type"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
