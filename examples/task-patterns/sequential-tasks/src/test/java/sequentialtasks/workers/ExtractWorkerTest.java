package sequentialtasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractWorkerTest {

    private final ExtractWorker worker = new ExtractWorker();

    @Test
    void taskDefName() {
        assertEquals("seq_extract", worker.getTaskDefName());
    }

    @Test
    void extractsDataFromSource() {
        Task task = taskWith(Map.of("source", "user_database"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("user_database", result.getOutputData().get("source"));
    }

    @Test
    void returnsThreeRecords() {
        Task task = taskWith(Map.of("source", "test_source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawData =
                (List<Map<String, Object>>) result.getOutputData().get("rawData");
        assertNotNull(rawData);
        assertEquals(3, rawData.size());
        assertEquals(3, result.getOutputData().get("recordCount"));
    }

    @Test
    void recordsContainExpectedFields() {
        Task task = taskWith(Map.of("source", "test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawData =
                (List<Map<String, Object>>) result.getOutputData().get("rawData");

        Map<String, Object> first = rawData.get(0);
        assertEquals(1, first.get("id"));
        assertEquals("Alice", first.get("name"));
        assertEquals(85, first.get("score"));

        Map<String, Object> second = rawData.get(1);
        assertEquals(2, second.get("id"));
        assertEquals("Bob", second.get("name"));
        assertEquals(92, second.get("score"));

        Map<String, Object> third = rawData.get(2);
        assertEquals(3, third.get("id"));
        assertEquals("Carol", third.get("name"));
        assertEquals(78, third.get("score"));
    }

    @Test
    void defaultsSourceWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default", result.getOutputData().get("source"));
    }

    @Test
    void defaultsSourceWhenBlank() {
        Task task = taskWith(Map.of("source", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("default", result.getOutputData().get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
