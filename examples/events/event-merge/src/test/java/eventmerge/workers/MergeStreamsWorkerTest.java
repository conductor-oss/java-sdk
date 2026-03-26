package eventmerge.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeStreamsWorkerTest {

    private final MergeStreamsWorker worker = new MergeStreamsWorker();

    @Test
    void taskDefName() {
        assertEquals("mg_merge_streams", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of(
                "streamA", List.of(Map.of("id", "a1")),
                "streamB", List.of(Map.of("id", "b1")),
                "streamC", List.of(Map.of("id", "c1"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void mergesAllThreeStreams() {
        List<Map<String, String>> streamA = List.of(
                Map.of("id", "a1", "source", "api", "type", "click"),
                Map.of("id", "a2", "source", "api", "type", "view"));
        List<Map<String, String>> streamB = List.of(
                Map.of("id", "b1", "source", "mobile", "type", "tap"),
                Map.of("id", "b2", "source", "mobile", "type", "scroll"),
                Map.of("id", "b3", "source", "mobile", "type", "tap"));
        List<Map<String, String>> streamC = List.of(
                Map.of("id", "c1", "source", "iot", "type", "sensor_reading"));

        Task task = taskWith(Map.of("streamA", streamA, "streamB", streamB, "streamC", streamC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> merged =
                (List<Map<String, String>>) result.getOutputData().get("merged");
        assertEquals(6, merged.size());
    }

    @Test
    void totalCountMatchesMergedSize() {
        List<Map<String, String>> streamA = List.of(Map.of("id", "a1"));
        List<Map<String, String>> streamB = List.of(Map.of("id", "b1"), Map.of("id", "b2"));
        List<Map<String, String>> streamC = List.of(Map.of("id", "c1"));

        Task task = taskWith(Map.of("streamA", streamA, "streamB", streamB, "streamC", streamC));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("totalCount"));
    }

    @Test
    void preservesEventOrder() {
        List<Map<String, String>> streamA = List.of(Map.of("id", "a1"));
        List<Map<String, String>> streamB = List.of(Map.of("id", "b1"));
        List<Map<String, String>> streamC = List.of(Map.of("id", "c1"));

        Task task = taskWith(Map.of("streamA", streamA, "streamB", streamB, "streamC", streamC));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> merged =
                (List<Map<String, String>>) result.getOutputData().get("merged");
        assertEquals("a1", merged.get(0).get("id"));
        assertEquals("b1", merged.get(1).get("id"));
        assertEquals("c1", merged.get(2).get("id"));
    }

    @Test
    void handlesNullStreamA() {
        Map<String, Object> input = new HashMap<>();
        input.put("streamA", null);
        input.put("streamB", List.of(Map.of("id", "b1")));
        input.put("streamC", List.of(Map.of("id", "c1")));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesAllNullStreams() {
        Map<String, Object> input = new HashMap<>();
        input.put("streamA", null);
        input.put("streamB", null);
        input.put("streamC", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> merged =
                (List<Map<String, String>>) result.getOutputData().get("merged");
        assertTrue(merged.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
