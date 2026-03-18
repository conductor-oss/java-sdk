package dataaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeAggregatesWorkerTest {

    private final ComputeAggregatesWorker worker = new ComputeAggregatesWorker();

    @Test
    void taskDefName() {
        assertEquals("agg_compute_aggregates", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void computesAggregatesForSingleGroup() {
        Map<String, List<Map<String, Object>>> groups = Map.of(
                "east", List.of(
                        Map.of("region", "east", "amount", 100),
                        Map.of("region", "east", "amount", 200)));
        Task task = taskWith(Map.of("groups", groups, "aggregateField", "amount"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        Map<String, Object> eastStats = aggregates.get("east");
        assertEquals(2, eastStats.get("count"));
        assertEquals(300.0, eastStats.get("sum"));
        assertEquals(150.0, eastStats.get("avg"));
        assertEquals(100.0, eastStats.get("min"));
        assertEquals(200.0, eastStats.get("max"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computesAggregatesForMultipleGroups() {
        Map<String, List<Map<String, Object>>> groups = Map.of(
                "east", List.of(Map.of("amount", 100)),
                "west", List.of(Map.of("amount", 200)));
        Task task = taskWith(Map.of("groups", groups, "aggregateField", "amount"));
        TaskResult result = worker.execute(task);

        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        assertEquals(2, aggregates.size());
        assertEquals(100.0, aggregates.get("east").get("sum"));
        assertEquals(200.0, aggregates.get("west").get("sum"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void computesAggregatesSingleRecord() {
        Map<String, List<Map<String, Object>>> groups = Map.of(
                "solo", List.of(Map.of("val", 42)));
        Task task = taskWith(Map.of("groups", groups, "aggregateField", "val"));
        TaskResult result = worker.execute(task);

        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        Map<String, Object> stats = aggregates.get("solo");
        assertEquals(1, stats.get("count"));
        assertEquals(42.0, stats.get("sum"));
        assertEquals(42.0, stats.get("avg"));
        assertEquals(42.0, stats.get("min"));
        assertEquals(42.0, stats.get("max"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesEmptyGroups() {
        Task task = taskWith(Map.of("groups", Map.of(), "aggregateField", "amount"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        assertTrue(aggregates.isEmpty());
    }

    @Test
    void handlesNullGroups() {
        Map<String, Object> input = new HashMap<>();
        input.put("groups", null);
        input.put("aggregateField", "amount");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullAggregateField() {
        Map<String, List<Map<String, Object>>> groups = Map.of(
                "g1", List.of(Map.of("value", 10)));
        Map<String, Object> input = new HashMap<>();
        input.put("groups", groups);
        input.put("aggregateField", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        Map<String, Object> stats = aggregates.get("g1");
        assertEquals(1, stats.get("count"));
        assertEquals(10.0, stats.get("sum"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesRecordsWithMissingAggregateField() {
        Map<String, List<Map<String, Object>>> groups = Map.of(
                "g1", List.of(
                        Map.of("amount", 100),
                        Map.of("other", "no-amount")));
        Task task = taskWith(Map.of("groups", groups, "aggregateField", "amount"));
        TaskResult result = worker.execute(task);

        Map<String, Map<String, Object>> aggregates =
                (Map<String, Map<String, Object>>) result.getOutputData().get("aggregates");
        Map<String, Object> stats = aggregates.get("g1");
        assertEquals(1, stats.get("count"));
        assertEquals(100.0, stats.get("sum"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
