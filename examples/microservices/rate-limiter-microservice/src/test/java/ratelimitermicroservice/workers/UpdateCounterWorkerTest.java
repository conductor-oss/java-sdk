package ratelimitermicroservice.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCounterWorkerTest {

    private final UpdateCounterWorker worker = new UpdateCounterWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_update_counter", worker.getTaskDefName());
    }

    @Test
    void updatesCounter() {
        Task task = taskWith(Map.of("clientId", "client-99", "endpoint", "/api/orders"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("updated"));
    }

    @Test
    void handlesNullClientId() {
        Map<String, Object> input = new HashMap<>();
        input.put("clientId", null);
        input.put("endpoint", "/api/test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("updated"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void updatedIsBoolean() {
        Task task = taskWith(Map.of("clientId", "c1", "endpoint", "/e"));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Boolean.class, result.getOutputData().get("updated"));
    }

    @Test
    void differentClientsAllUpdate() {
        Task task1 = taskWith(Map.of("clientId", "c1", "endpoint", "/a"));
        Task task2 = taskWith(Map.of("clientId", "c2", "endpoint", "/b"));

        assertEquals(true, worker.execute(task1).getOutputData().get("updated"));
        assertEquals(true, worker.execute(task2).getOutputData().get("updated"));
    }

    @Test
    void outputContainsUpdatedKey() {
        Task task = taskWith(Map.of("clientId", "c1", "endpoint", "/x"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("updated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
