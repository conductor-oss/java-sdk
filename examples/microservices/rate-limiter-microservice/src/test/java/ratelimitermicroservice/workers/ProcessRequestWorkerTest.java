package ratelimitermicroservice.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessRequestWorkerTest {

    private final ProcessRequestWorker worker = new ProcessRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_process_request", worker.getTaskDefName());
    }

    @Test
    void processesSuccessfully() {
        Task task = taskWith(Map.of("request", Map.of("action", "list")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void responseHasStatusOk() {
        Task task = taskWith(Map.of("request", Map.of("action", "list")));
        TaskResult result = worker.execute(task);

        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertEquals("ok", response.get("status"));
    }

    @Test
    void handlesMissingRequest() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void responseIsMap() {
        Task task = taskWith(Map.of("request", Map.of("action", "create")));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Map.class, result.getOutputData().get("response"));
    }

    @Test
    void outputContainsResponseKey() {
        Task task = taskWith(Map.of("request", Map.of("action", "delete")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
    }

    @Test
    void multipleCallsReturnSameResponse() {
        Task task1 = taskWith(Map.of("request", Map.of("action", "a")));
        Task task2 = taskWith(Map.of("request", Map.of("action", "b")));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("response"), r2.getOutputData().get("response"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
