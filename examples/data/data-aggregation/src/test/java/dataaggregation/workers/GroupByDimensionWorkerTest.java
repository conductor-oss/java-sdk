package dataaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupByDimensionWorkerTest {

    private final GroupByDimensionWorker worker = new GroupByDimensionWorker();

    @Test
    void taskDefName() {
        assertEquals("agg_group_by_dimension", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupsByRegion() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("region", "east", "amount", 100),
                        Map.of("region", "west", "amount", 200),
                        Map.of("region", "east", "amount", 150)),
                "groupBy", "region"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, List<Map<String, Object>>> groups =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(2, groups.size());
        assertEquals(2, groups.get("east").size());
        assertEquals(1, groups.get("west").size());
    }

    @Test
    void returnsGroupCount() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("dept", "eng", "salary", 100000),
                        Map.of("dept", "sales", "salary", 80000),
                        Map.of("dept", "eng", "salary", 110000)),
                "groupBy", "dept"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("groupCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(),
                "groupBy", "region"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("groupCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesMissingGroupByField() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("name", "Alice")));
        input.put("groupBy", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, List<Map<String, Object>>> groups =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertTrue(groups.containsKey("unknown"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("groupBy", "region");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("groupCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupsAllIntoOneWhenSameKey() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("type", "A", "val", 1),
                        Map.of("type", "A", "val", 2),
                        Map.of("type", "A", "val", 3)),
                "groupBy", "type"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("groupCount"));
        Map<String, List<Map<String, Object>>> groups =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(3, groups.get("A").size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void groupsByFieldNotPresentInSomeRecords() {
        Task task = taskWith(Map.of(
                "records", List.of(
                        Map.of("region", "east", "amount", 100),
                        Map.of("amount", 200)),
                "groupBy", "region"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, List<Map<String, Object>>> groups =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("groups");
        assertEquals(2, groups.size());
        assertTrue(groups.containsKey("east"));
        assertTrue(groups.containsKey("unknown"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
