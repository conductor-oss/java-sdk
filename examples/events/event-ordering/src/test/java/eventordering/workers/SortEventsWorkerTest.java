package eventordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SortEventsWorkerTest {

    private final SortEventsWorker worker = new SortEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("oo_sort_events", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("buffered", List.of(
                Map.of("seq", 3, "type", "update", "data", "third"),
                Map.of("seq", 1, "type", "create", "data", "first"),
                Map.of("seq", 2, "type", "modify", "data", "second")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsFixedSortedOrder() {
        Task task = taskWith(Map.of("buffered", List.of(
                Map.of("seq", 3, "type", "update", "data", "third"),
                Map.of("seq", 1, "type", "create", "data", "first"),
                Map.of("seq", 2, "type", "modify", "data", "second")
        )));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> sorted =
                (List<Map<String, Object>>) result.getOutputData().get("sorted");
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).get("seq"));
        assertEquals(2, sorted.get(1).get("seq"));
        assertEquals(3, sorted.get(2).get("seq"));
    }

    @Test
    void returnsSortFieldSeq() {
        Task task = taskWith(Map.of("buffered", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("seq", result.getOutputData().get("sortField"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void firstSortedEventIsCreate() {
        Task task = taskWith(Map.of("buffered", List.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> sorted =
                (List<Map<String, Object>>) result.getOutputData().get("sorted");
        assertEquals("create", sorted.get(0).get("type"));
        assertEquals("first", sorted.get(0).get("data"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void secondSortedEventIsModify() {
        Task task = taskWith(Map.of("buffered", List.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> sorted =
                (List<Map<String, Object>>) result.getOutputData().get("sorted");
        assertEquals("modify", sorted.get(1).get("type"));
        assertEquals("second", sorted.get(1).get("data"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void thirdSortedEventIsUpdate() {
        Task task = taskWith(Map.of("buffered", List.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> sorted =
                (List<Map<String, Object>>) result.getOutputData().get("sorted");
        assertEquals("update", sorted.get(2).get("type"));
        assertEquals("third", sorted.get(2).get("data"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sortedOutputIsDeterministic() {
        Task task1 = taskWith(Map.of("buffered", List.of()));
        Task task2 = taskWith(Map.of("buffered", List.of()));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        List<Map<String, Object>> sorted1 =
                (List<Map<String, Object>>) result1.getOutputData().get("sorted");
        List<Map<String, Object>> sorted2 =
                (List<Map<String, Object>>) result2.getOutputData().get("sorted");
        assertEquals(sorted1, sorted2);
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sorted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
