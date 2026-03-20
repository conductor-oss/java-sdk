package ratelimitermicroservice.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RejectRequestWorkerTest {

    private final RejectRequestWorker worker = new RejectRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_reject_request", worker.getTaskDefName());
    }

    @Test
    void rejectsRequest() {
        Task task = taskWith(Map.of("clientId", "client-99", "retryAfter", 30));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
    }

    @Test
    void retryAfterIs30() {
        Task task = taskWith(Map.of("clientId", "client-99", "retryAfter", 60));
        TaskResult result = worker.execute(task);

        assertEquals(30, result.getOutputData().get("retryAfter"));
    }

    @Test
    void handlesNullClientId() {
        Map<String, Object> input = new HashMap<>();
        input.put("clientId", null);
        input.put("retryAfter", 30);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("rejected"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void rejectedIsBoolean() {
        Task task = taskWith(Map.of("clientId", "c1", "retryAfter", 10));
        TaskResult result = worker.execute(task);

        assertInstanceOf(Boolean.class, result.getOutputData().get("rejected"));
    }

    @Test
    void outputHasBothKeys() {
        Task task = taskWith(Map.of("clientId", "c1", "retryAfter", 10));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("rejected"));
        assertTrue(result.getOutputData().containsKey("retryAfter"));
    }

    @Test
    void multipleRejectionsAllSucceed() {
        Task task1 = taskWith(Map.of("clientId", "c1", "retryAfter", 5));
        Task task2 = taskWith(Map.of("clientId", "c2", "retryAfter", 10));

        assertEquals(true, worker.execute(task1).getOutputData().get("rejected"));
        assertEquals(true, worker.execute(task2).getOutputData().get("rejected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
