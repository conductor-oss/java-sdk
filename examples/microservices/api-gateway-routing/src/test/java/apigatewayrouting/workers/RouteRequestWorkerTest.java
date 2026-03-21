package apigatewayrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteRequestWorkerTest {

    private final RouteRequestWorker worker = new RouteRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("gw_route_request", worker.getTaskDefName());
    }

    @Test
    void routesGetRequest() {
        Task task = taskWith(Map.of("path", "/api/orders/123", "method", "GET", "body", Map.of(), "clientId", "client-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    @Test
    void responseContainsOrderData() {
        Task task = taskWith(Map.of("path", "/api/orders/123", "method", "GET"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        assertEquals("ORD-123", response.get("orderId"));
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void handlesNullMethod() {
        Map<String, Object> input = new HashMap<>();
        input.put("method", null);
        input.put("path", "/api/test");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullPath() {
        Map<String, Object> input = new HashMap<>();
        input.put("path", null);
        input.put("method", "POST");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("path", "/test", "method", "GET"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("statusCode"));
        assertTrue(result.getOutputData().containsKey("response"));
    }

    @Test
    void statusCodeIs200() {
        Task task = taskWith(Map.of("path", "/any", "method", "POST"));
        TaskResult result = worker.execute(task);

        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
