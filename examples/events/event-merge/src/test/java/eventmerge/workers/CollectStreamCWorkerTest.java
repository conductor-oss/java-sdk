package eventmerge.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectStreamCWorkerTest {

    private final CollectStreamCWorker worker = new CollectStreamCWorker();

    @Test
    void taskDefName() {
        assertEquals("mg_collect_stream_c", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("source", "iot"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsOneEvent() {
        Task task = taskWith(Map.of("source", "iot"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals(1, events.size());
    }

    @Test
    void returnsCountMatchingEvents() {
        Task task = taskWith(Map.of("source", "iot"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    void eventHasCorrectFields() {
        Task task = taskWith(Map.of("source", "iot"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        Map<String, String> event = events.get(0);
        assertEquals("c1", event.get("id"));
        assertEquals("iot", event.get("source"));
        assertEquals("sensor_reading", event.get("type"));
    }

    @Test
    void eventSourceIsIot() {
        Task task = taskWith(Map.of("source", "iot"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events =
                (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals("iot", events.get(0).get("source"));
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
        assertEquals(1, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
