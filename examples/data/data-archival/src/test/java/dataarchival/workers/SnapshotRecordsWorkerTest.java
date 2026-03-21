package dataarchival.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotRecordsWorkerTest {

    private final SnapshotRecordsWorker worker = new SnapshotRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("arc_snapshot_records", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void createsSnapshot() {
        List<Map<String, Object>> records = List.of(Map.of("id", "R1", "name", "old data"));
        Task task = taskWith(Map.of("staleRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> snapshot = (Map<String, Object>) result.getOutputData().get("snapshot");
        assertNotNull(snapshot);
        assertEquals(1, snapshot.get("recordCount"));
        assertTrue((int) snapshot.get("sizeBytes") > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void snapshotContainsTimestamp() {
        Task task = taskWith(Map.of("staleRecords", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);

        Map<String, Object> snapshot = (Map<String, Object>) result.getOutputData().get("snapshot");
        assertNotNull(snapshot.get("timestamp"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("staleRecords", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("staleRecords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void snapshotContainsRecords() {
        List<Map<String, Object>> records = List.of(Map.of("id", "R1"), Map.of("id", "R2"));
        Task task = taskWith(Map.of("staleRecords", records));
        TaskResult result = worker.execute(task);

        Map<String, Object> snapshot = (Map<String, Object>) result.getOutputData().get("snapshot");
        assertEquals(2, snapshot.get("recordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
