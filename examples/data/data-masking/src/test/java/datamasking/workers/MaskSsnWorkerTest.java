package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaskSsnWorkerTest {

    private final MaskSsnWorker worker = new MaskSsnWorker();

    @Test
    void taskDefName() {
        assertEquals("mk_mask_ssn", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void masksSsn() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("name", "Alice", "ssn", "123-45-6789")));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> masked = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("***-**-6789", masked.get(0).get("ssn"));
        assertEquals(1, result.getOutputData().get("maskedCount"));
    }

    @Test
    void skipsRecordsWithoutSsn() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("name", "Alice", "email", "a@b.com")));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("maskedCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("maskedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
