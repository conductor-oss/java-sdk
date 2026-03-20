package webhookcallback.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveRequestWorkerTest {

    private final ReceiveRequestWorker worker = new ReceiveRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("wc_receive_request", worker.getTaskDefName());
    }

    @Test
    void receivesStandardRequest() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-001",
                "data", Map.of("type", "customer_import", "recordCount", 150)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-fixed-001", result.getOutputData().get("requestId"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("receivedAt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsParsedData() {
        Task task = taskWith(Map.of(
                "requestId", "req-001",
                "data", Map.of("type", "customer_import", "recordCount", 150)));
        TaskResult result = worker.execute(task);

        Map<String, Object> parsedData = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertNotNull(parsedData);
        assertEquals("customer_import", parsedData.get("type"));
        assertEquals(150, parsedData.get("records"));
        assertEquals("json", parsedData.get("format"));
        assertEquals(48200, parsedData.get("sizeBytes"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesZeroRecordCount() {
        Task task = taskWith(Map.of(
                "requestId", "req-002",
                "data", Map.of("type", "batch_update", "recordCount", 0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> parsedData = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertEquals(0, parsedData.get("records"));
        assertEquals("batch_update", parsedData.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesMissingData() {
        Task task = taskWith(Map.of("requestId", "req-003"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> parsedData = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertNotNull(parsedData);
        assertEquals("unknown", parsedData.get("type"));
        assertEquals(0, parsedData.get("records"));
    }

    @Test
    void handlesMissingRequestId() {
        Task task = taskWith(Map.of(
                "data", Map.of("type", "sync", "recordCount", 10)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("requestId"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("requestId"));
        assertNotNull(result.getOutputData().get("parsedData"));
    }

    @Test
    void handlesNullRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", null);
        input.put("data", Map.of("type", "test", "recordCount", 5));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("requestId"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesRequestIdInOutput() {
        Task task = taskWith(Map.of(
                "requestId", "custom-id-xyz",
                "data", Map.of("type", "export", "recordCount", 500)));
        TaskResult result = worker.execute(task);

        assertEquals("custom-id-xyz", result.getOutputData().get("requestId"));
        Map<String, Object> parsedData = (Map<String, Object>) result.getOutputData().get("parsedData");
        assertEquals(500, parsedData.get("records"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
