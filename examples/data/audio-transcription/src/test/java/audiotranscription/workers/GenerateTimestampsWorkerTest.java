package audiotranscription.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTimestampsWorkerTest {

    private final GenerateTimestampsWorker worker = new GenerateTimestampsWorker();

    @Test
    void taskDefName() {
        assertEquals("au_generate_timestamps", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void generatesTimestamps() {
        List<Map<String, Object>> segments = List.of(
                Map.of("speaker", "Speaker 1", "text", "Hello"),
                Map.of("speaker", "Speaker 2", "text", "Hi there"));
        Task task = taskWith(Map.of("segments", segments, "transcript", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("segmentCount"));
        List<Map<String, Object>> timestamped = (List<Map<String, Object>>) result.getOutputData().get("timestamped");
        assertNotNull(timestamped.get(0).get("startTime"));
        assertNotNull(timestamped.get(0).get("endTime"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalData() {
        List<Map<String, Object>> segments = List.of(
                Map.of("speaker", "Speaker 1", "text", "Test text"));
        Task task = taskWith(Map.of("segments", segments));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> timestamped = (List<Map<String, Object>>) result.getOutputData().get("timestamped");
        assertEquals("Speaker 1", timestamped.get(0).get("speaker"));
        assertEquals("Test text", timestamped.get(0).get("text"));
    }

    @Test
    void handlesEmptySegments() {
        Task task = taskWith(Map.of("segments", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("segmentCount"));
    }

    @Test
    void handlesNullSegments() {
        Map<String, Object> input = new HashMap<>();
        input.put("segments", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("segmentCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void timestampsAreSequential() {
        List<Map<String, Object>> segments = List.of(
                Map.of("speaker", "S1", "text", "A"),
                Map.of("speaker", "S2", "text", "B"),
                Map.of("speaker", "S1", "text", "C"));
        Task task = taskWith(Map.of("segments", segments));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> timestamped = (List<Map<String, Object>>) result.getOutputData().get("timestamped");
        assertEquals(3, timestamped.size());
        // First segment starts at 0:
        assertTrue(((String) timestamped.get(0).get("startTime")).startsWith("0:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
