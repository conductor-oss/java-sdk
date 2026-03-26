package approvaldashboardnextjs.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NxtProcessWorkerTest {

    @Test
    void taskDefName() {
        NxtProcessWorker worker = new NxtProcessWorker();
        assertEquals("nxt_process", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrue() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "expense",
                "title", "Q4 Budget",
                "amount", 15000,
                "requester", "alice@example.com"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsAllInputFields() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "purchase",
                "title", "Server Hardware",
                "amount", 8500,
                "requester", "bob@example.com"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("purchase", result.getOutputData().get("type"));
        assertEquals("Server Hardware", result.getOutputData().get("title"));
        assertEquals(8500, result.getOutputData().get("amount"));
        assertEquals("bob@example.com", result.getOutputData().get("requester"));
    }

    @Test
    void handlesDefaultsWhenInputsMissing() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("unknown", result.getOutputData().get("type"));
        assertEquals("untitled", result.getOutputData().get("title"));
        assertEquals(0, result.getOutputData().get("amount"));
        assertEquals("unknown", result.getOutputData().get("requester"));
    }

    @Test
    void handlesPartialInputs() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "travel",
                "title", "Conference Trip"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("travel", result.getOutputData().get("type"));
        assertEquals("Conference Trip", result.getOutputData().get("title"));
        assertEquals(0, result.getOutputData().get("amount"));
        assertEquals("unknown", result.getOutputData().get("requester"));
    }

    @Test
    void outputContainsProcessedKey() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "expense",
                "title", "Office Supplies",
                "amount", 500,
                "requester", "carol@example.com"
        ));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
        assertTrue(result.getOutputData().containsKey("type"));
        assertTrue(result.getOutputData().containsKey("title"));
        assertTrue(result.getOutputData().containsKey("amount"));
        assertTrue(result.getOutputData().containsKey("requester"));
    }

    @Test
    void alwaysCompletesSuccessfully() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "expense",
                "title", "Team Lunch",
                "amount", 200,
                "requester", "dave@example.com"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void preservesAmountAsNumber() {
        NxtProcessWorker worker = new NxtProcessWorker();
        Task task = taskWith(Map.of(
                "type", "purchase",
                "title", "Laptop",
                "amount", 2500,
                "requester", "eve@example.com"
        ));

        TaskResult result = worker.execute(task);

        assertEquals(2500, result.getOutputData().get("amount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
