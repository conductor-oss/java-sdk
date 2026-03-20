package datapartitioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPartitionAWorkerTest {

    private final ProcessPartitionAWorker worker = new ProcessPartitionAWorker();

    @Test
    void taskDefName() {
        assertEquals("par_process_partition_a", worker.getTaskDefName());
    }

    @Test
    void processesMultipleRecords() {
        Task task = taskWith(Map.of(
                "partition", List.of(
                        Map.of("id", 1, "value", "alpha"),
                        Map.of("id", 2, "value", "beta")),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(2, records.size());
        assertEquals(2, result.getOutputData().get("processedCount"));
    }

    @Test
    void addsProcessedFlag() {
        Task task = taskWith(Map.of(
                "partition", List.of(Map.of("id", 1, "value", "test")),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertTrue((Boolean) records.get(0).get("processed"));
    }

    @Test
    void addsPartitionLabel() {
        Task task = taskWith(Map.of(
                "partition", List.of(Map.of("id", 1, "value", "test")),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals("A", records.get(0).get("partition"));
    }

    @Test
    void preservesOriginalFields() {
        Task task = taskWith(Map.of(
                "partition", List.of(Map.of("id", 42, "value", "preserved", "extra", "data")),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(42, records.get(0).get("id"));
        assertEquals("preserved", records.get(0).get("value"));
        assertEquals("data", records.get(0).get("extra"));
    }

    @Test
    void handlesEmptyPartition() {
        Task task = taskWith(Map.of(
                "partition", List.of(),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(0, records.size());
        assertEquals(0, result.getOutputData().get("processedCount"));
    }

    @Test
    void handlesNullPartition() {
        Map<String, Object> input = new HashMap<>();
        input.put("partition", null);
        input.put("partitionName", "A");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(0, records.size());
    }

    @Test
    void processedCountMatchesResultSize() {
        Task task = taskWith(Map.of(
                "partition", List.of(
                        Map.of("id", 1),
                        Map.of("id", 2),
                        Map.of("id", 3)),
                "partitionName", "A"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(records.size(), result.getOutputData().get("processedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
