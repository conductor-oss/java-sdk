package datapartitioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPartitionBWorkerTest {

    private final ProcessPartitionBWorker worker = new ProcessPartitionBWorker();

    @Test
    void taskDefName() {
        assertEquals("par_process_partition_b", worker.getTaskDefName());
    }

    @Test
    void processesMultipleRecords() {
        Task task = taskWith(Map.of(
                "partition", List.of(
                        Map.of("id", 3, "value", "gamma"),
                        Map.of("id", 4, "value", "delta")),
                "partitionName", "B"));
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
                "partition", List.of(Map.of("id", 5, "value", "epsilon")),
                "partitionName", "B"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertTrue((Boolean) records.get(0).get("processed"));
    }

    @Test
    void addsPartitionLabel() {
        Task task = taskWith(Map.of(
                "partition", List.of(Map.of("id", 5, "value", "epsilon")),
                "partitionName", "B"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals("B", records.get(0).get("partition"));
    }

    @Test
    void preservesOriginalFields() {
        Task task = taskWith(Map.of(
                "partition", List.of(Map.of("id", 99, "value", "kept", "tag", "important")),
                "partitionName", "B"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("result");
        assertEquals(99, records.get(0).get("id"));
        assertEquals("kept", records.get(0).get("value"));
        assertEquals("important", records.get(0).get("tag"));
    }

    @Test
    void handlesEmptyPartition() {
        Task task = taskWith(Map.of(
                "partition", List.of(),
                "partitionName", "B"));
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
        input.put("partitionName", "B");
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
                        Map.of("id", 10),
                        Map.of("id", 11),
                        Map.of("id", 12),
                        Map.of("id", 13)),
                "partitionName", "B"));
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
