package datapartitioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeResultsWorkerTest {

    private final MergeResultsWorker worker = new MergeResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("par_merge_results", worker.getTaskDefName());
    }

    @Test
    void mergesBothPartitions() {
        Task task = taskWith(Map.of(
                "resultA", List.of(
                        Map.of("id", 1, "partition", "A"),
                        Map.of("id", 2, "partition", "A")),
                "resultB", List.of(
                        Map.of("id", 3, "partition", "B")),
                "countA", 2,
                "countB", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(3, records.size());
        assertEquals(3, result.getOutputData().get("mergedCount"));
    }

    @Test
    void generatesSummaryString() {
        Task task = taskWith(Map.of(
                "resultA", List.of(Map.of("id", 1)),
                "resultB", List.of(Map.of("id", 2), Map.of("id", 3)),
                "countA", 1,
                "countB", 2));
        TaskResult result = worker.execute(task);

        assertEquals("Merged 1 + 2 = 3 records from 2 partitions",
                result.getOutputData().get("summary"));
    }

    @Test
    void handlesEmptyResultA() {
        Task task = taskWith(Map.of(
                "resultA", List.of(),
                "resultB", List.of(Map.of("id", 1), Map.of("id", 2)),
                "countA", 0,
                "countB", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("mergedCount"));
        assertEquals("Merged 0 + 2 = 2 records from 2 partitions",
                result.getOutputData().get("summary"));
    }

    @Test
    void handlesEmptyResultB() {
        Task task = taskWith(Map.of(
                "resultA", List.of(Map.of("id", 1)),
                "resultB", List.of(),
                "countA", 1,
                "countB", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("mergedCount"));
    }

    @Test
    void handlesBothEmpty() {
        Task task = taskWith(Map.of(
                "resultA", List.of(),
                "resultB", List.of(),
                "countA", 0,
                "countB", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("mergedCount"));
        assertEquals("Merged 0 + 0 = 0 records from 2 partitions",
                result.getOutputData().get("summary"));
    }

    @Test
    void handlesNullResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", null);
        input.put("resultB", null);
        input.put("countA", 0);
        input.put("countB", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("mergedCount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("mergedCount"));
        assertNotNull(result.getOutputData().get("summary"));
        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void preservesRecordOrder() {
        Task task = taskWith(Map.of(
                "resultA", List.of(
                        Map.of("id", 1, "value", "first"),
                        Map.of("id", 2, "value", "second")),
                "resultB", List.of(
                        Map.of("id", 3, "value", "third"),
                        Map.of("id", 4, "value", "fourth")),
                "countA", 2,
                "countB", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(4, records.size());
        assertEquals("first", records.get(0).get("value"));
        assertEquals("second", records.get(1).get("value"));
        assertEquals("third", records.get(2).get("value"));
        assertEquals("fourth", records.get(3).get("value"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
