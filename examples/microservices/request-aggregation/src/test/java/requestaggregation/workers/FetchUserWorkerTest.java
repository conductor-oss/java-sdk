package requestaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchUserWorkerTest {

    private final FetchUserWorker worker = new FetchUserWorker();

    @Test
    void taskDefName() { assertEquals("agg_fetch_user", worker.getTaskDefName()); }

    @Test
    void fetchesUserProfile() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Alice", result.getOutputData().get("name"));
        assertEquals("alice@co.com", result.getOutputData().get("email"));
        assertEquals("premium", result.getOutputData().get("tier"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>(); input.put("userId", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllFields() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        assertTrue(result.getOutputData().containsKey("name"));
        assertTrue(result.getOutputData().containsKey("email"));
        assertTrue(result.getOutputData().containsKey("tier"));
    }

    @Test
    void isDeterministic() {
        TaskResult r1 = worker.execute(taskWith(Map.of("userId", "a")));
        TaskResult r2 = worker.execute(taskWith(Map.of("userId", "b")));
        assertEquals(r1.getOutputData().get("name"), r2.getOutputData().get("name"));
    }

    @Test
    void tierIsPremium() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "any")));
        assertEquals("premium", result.getOutputData().get("tier"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
