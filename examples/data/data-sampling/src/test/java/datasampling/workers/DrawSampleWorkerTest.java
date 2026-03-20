package datasampling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DrawSampleWorkerTest {

    private final DrawSampleWorker worker = new DrawSampleWorker();

    @Test
    void taskDefName() {
        assertEquals("sm_draw_sample", worker.getTaskDefName());
    }

    @Test
    void drawsHalfSample() {
        // sampleRate=0.5 -> step=floor(1/0.5)=2 -> indices 0,2
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2),
                Map.of("name", "C", "value", 3),
                Map.of("name", "D", "value", 4));
        Task task = taskWith(Map.of("records", records, "sampleRate", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(2, sample.size());
        assertEquals("A", sample.get(0).get("name"));
        assertEquals("C", sample.get(1).get("name"));
        assertEquals(2, result.getOutputData().get("sampleSize"));
    }

    @Test
    void drawsFullSampleAtRateOne() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2));
        Task task = taskWith(Map.of("records", records, "sampleRate", 1.0));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(2, sample.size());
        assertEquals(2, result.getOutputData().get("sampleSize"));
    }

    @Test
    void drawsEmptySampleAtRateZero() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1));
        Task task = taskWith(Map.of("records", records, "sampleRate", 0.0));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(0, sample.size());
        assertEquals(0, result.getOutputData().get("sampleSize"));
    }

    @Test
    void drawsQuarterSample() {
        // sampleRate=0.25 -> step=floor(1/0.25)=4 -> indices 0,4
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2),
                Map.of("name", "C", "value", 3),
                Map.of("name", "D", "value", 4),
                Map.of("name", "E", "value", 5));
        Task task = taskWith(Map.of("records", records, "sampleRate", 0.25));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(2, sample.size());
        assertEquals("A", sample.get(0).get("name"));
        assertEquals("E", sample.get(1).get("name"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "sampleRate", 0.5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sampleSize"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("sampleRate", 0.5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sampleSize"));
    }

    @Test
    void handlesNullSampleRateDefaultsToOne() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2));
        Map<String, Object> input = new HashMap<>();
        input.put("records", records);
        input.put("sampleRate", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        // default sampleRate 1.0 -> all records
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(2, sample.size());
    }

    @Test
    void handlesRateAboveOne() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A", "value", 1),
                Map.of("name", "B", "value", 2));
        Task task = taskWith(Map.of("records", records, "sampleRate", 2.0));
        TaskResult result = worker.execute(task);

        // sampleRate >= 1.0 -> returns all records
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sample = (List<Map<String, Object>>) result.getOutputData().get("sample");
        assertEquals(2, sample.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
