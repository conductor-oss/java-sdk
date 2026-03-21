package eventordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BufferEventsWorkerTest {

    private final BufferEventsWorker worker = new BufferEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("oo_buffer_events", worker.getTaskDefName());
    }

    @Test
    void buffersThreeEvents() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("seq", 3, "type", "update", "data", "third"),
                Map.of("seq", 1, "type", "create", "data", "first"),
                Map.of("seq", 2, "type", "modify", "data", "second")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void outputBufferedMatchesInput() {
        List<Map<String, Object>> events = List.of(
                Map.of("seq", 3, "type", "update", "data", "third"),
                Map.of("seq", 1, "type", "create", "data", "first"),
                Map.of("seq", 2, "type", "modify", "data", "second")
        );
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> buffered =
                (List<Map<String, Object>>) result.getOutputData().get("buffered");
        assertEquals(3, buffered.size());
        assertEquals(3, buffered.get(0).get("seq"));
        assertEquals(1, buffered.get(1).get("seq"));
        assertEquals(2, buffered.get(2).get("seq"));
    }

    @Test
    void handlesEmptyEventsList() {
        Task task = taskWith(Map.of("events", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
        assertEquals(List.of(), result.getOutputData().get("buffered"));
    }

    @Test
    void handlesMissingEventsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void buffersSingleEvent() {
        Task task = taskWith(Map.of("events", List.of(
                Map.of("seq", 1, "type", "create", "data", "only")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesEventOrder() {
        List<Map<String, Object>> events = List.of(
                Map.of("seq", 5, "type", "delete", "data", "fifth"),
                Map.of("seq", 2, "type", "modify", "data", "second")
        );
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> buffered =
                (List<Map<String, Object>>) result.getOutputData().get("buffered");
        assertEquals(5, buffered.get(0).get("seq"));
        assertEquals(2, buffered.get(1).get("seq"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
