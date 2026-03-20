package datapartitioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitDataWorkerTest {

    private final SplitDataWorker worker = new SplitDataWorker();

    @Test
    void taskDefName() {
        assertEquals("par_split_data", worker.getTaskDefName());
    }

    @Test
    void splitsEvenNumberOfRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("id", 1, "value", "a"),
                        Map.of("id", 2, "value", "b"),
                        Map.of("id", 3, "value", "c"),
                        Map.of("id", 4, "value", "d")),
                "partitionKey", "id"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partA = (List<Map<String, Object>>) result.getOutputData().get("partitionA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partB = (List<Map<String, Object>>) result.getOutputData().get("partitionB");
        assertEquals(2, partA.size());
        assertEquals(2, partB.size());
        assertEquals(4, result.getOutputData().get("totalCount"));
    }

    @Test
    void splitsOddNumberOfRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("id", 1, "value", "x"),
                        Map.of("id", 2, "value", "y"),
                        Map.of("id", 3, "value", "z")),
                "partitionKey", "id"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partA = (List<Map<String, Object>>) result.getOutputData().get("partitionA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partB = (List<Map<String, Object>>) result.getOutputData().get("partitionB");
        // ceil(3/2) = 2 for A, 1 for B
        assertEquals(2, partA.size());
        assertEquals(1, partB.size());
    }

    @Test
    void splitsSingleRecord() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("id", 1, "value", "solo")),
                "partitionKey", "id"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partA = (List<Map<String, Object>>) result.getOutputData().get("partitionA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partB = (List<Map<String, Object>>) result.getOutputData().get("partitionB");
        assertEquals(1, partA.size());
        assertEquals(0, partB.size());
        assertEquals(1, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesEmptyRecordsList() {
        Task task = taskWith(Map.of(
                "records", List.of(),
                "partitionKey", "id"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partA = (List<Map<String, Object>>) result.getOutputData().get("partitionA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partB = (List<Map<String, Object>>) result.getOutputData().get("partitionB");
        assertEquals(0, partA.size());
        assertEquals(0, partB.size());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("partitionKey", "id");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    @Test
    void handlesNullPartitionKey() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("id", 1)));
        input.put("partitionKey", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalCount"));
    }

    @Test
    void outputContainsTotalCount() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("id", 1),
                        Map.of("id", 2),
                        Map.of("id", 3),
                        Map.of("id", 4),
                        Map.of("id", 5)),
                "partitionKey", "id"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("totalCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partA = (List<Map<String, Object>>) result.getOutputData().get("partitionA");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partB = (List<Map<String, Object>>) result.getOutputData().get("partitionB");
        // ceil(5/2) = 3 for A, 2 for B
        assertEquals(3, partA.size());
        assertEquals(2, partB.size());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
