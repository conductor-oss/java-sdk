package apigatewayrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticateWorkerTest {

    private final AuthenticateWorker worker = new AuthenticateWorker();

    @Test
    void taskDefName() {
        assertEquals("gw_authenticate", worker.getTaskDefName());
    }

    @Test
    void authenticatesSuccessfully() {
        Task task = taskWith(Map.of("authorization", "Bearer token123", "path", "/api/orders"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("client-42", result.getOutputData().get("clientId"));
        assertEquals("v2", result.getOutputData().get("clientVersion"));
    }

    @Test
    void returnsRequestId() {
        Task task = taskWith(Map.of("authorization", "Bearer abc", "path", "/api/users"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("requestId"));
    }

    @Test
    void requestIdIsDeterministic() {
        Task t1 = taskWith(Map.of("path", "/api/a"));
        Task t2 = taskWith(Map.of("path", "/api/b"));
        assertEquals(worker.execute(t1).getOutputData().get("requestId"),
                     worker.execute(t2).getOutputData().get("requestId"));
    }

    @Test
    void handlesNullPath() {
        Map<String, Object> input = new HashMap<>();
        input.put("path", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("client-42", result.getOutputData().get("clientId"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("authorization", "Bearer x", "path", "/test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("clientId"));
        assertTrue(result.getOutputData().containsKey("clientVersion"));
        assertTrue(result.getOutputData().containsKey("requestId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
