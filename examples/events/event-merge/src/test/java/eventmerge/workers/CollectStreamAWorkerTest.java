package eventmerge.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectStreamAWorkerTest {

    private final CollectStreamAWorker worker = new CollectStreamAWorker();

    @Test
    void taskDefName() {
        assertEquals("mg_collect_stream_a", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("source", "api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsTwoEvents() {
        Task task = taskWith(Map.of("source", "api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals(2, events.size());
    }

    @Test
    void returnsCountMatchingEvents() {
        Task task = taskWith(Map.of("source", "api"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void firstEventHasCorrectFields() {
        Task task = taskWith(Map.of("source", "api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        Map<String, String> first = events.get(0);
        assertEquals("a1", first.get("id"));
        assertEquals("api", first.get("source"));
        assertEquals("click", first.get("type"));
    }

    @Test
    void secondEventHasCorrectFields() {
        Task task = taskWith(Map.of("source", "api"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        Map<String, String> second = events.get(1);
        assertEquals("a2", second.get("id"));
        assertEquals("api", second.get("source"));
        assertEquals("view", second.get("type"));
    }

    @Test
    void handlesNullSource() {
        Map<String, Object> input = new HashMap<>();
        input.put("source", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void handlesMissingSource() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
