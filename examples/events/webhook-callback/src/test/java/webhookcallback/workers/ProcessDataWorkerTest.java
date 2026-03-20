package webhookcallback.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDataWorkerTest {

    private final ProcessDataWorker worker = new ProcessDataWorker();

    @Test
    void taskDefName() {
        assertEquals("wc_process_data", worker.getTaskDefName());
    }

    @Test
    void processesStandardData() {
        Task task = taskWith(Map.of(
                "requestId", "req-fixed-001",
                "parsedData", Map.of("type", "customer_import", "records", 150, "format", "json", "sizeBytes", 48200)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("req-fixed-001", result.getOutputData().get("requestId"));
        assertEquals("completed", result.getOutputData().get("processingStatus"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsCorrectRecordCounts() {
        Task task = taskWith(Map.of(
                "requestId", "req-001",
                "parsedData", Map.of("records", 150)));
        TaskResult result = worker.execute(task);

        Map<String, Object> processingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(processingResult);
        assertEquals(150, processingResult.get("recordsProcessed"));
        assertEquals(148, processingResult.get("recordsSucceeded"));
        assertEquals(2, processingResult.get("recordsFailed"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsProcessingMetadata() {
        Task task = taskWith(Map.of(
                "requestId", "req-002",
                "parsedData", Map.of("records", 100)));
        TaskResult result = worker.execute(task);

        Map<String, Object> processingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(1847, processingResult.get("processingTimeMs"));
        assertEquals("normalized_json", processingResult.get("outputFormat"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesZeroRecords() {
        Task task = taskWith(Map.of(
                "requestId", "req-003",
                "parsedData", Map.of("records", 0)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> processingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(0, processingResult.get("recordsProcessed"));
        assertEquals(-2, processingResult.get("recordsSucceeded"));
        assertEquals(2, processingResult.get("recordsFailed"));
    }

    @Test
    void handlesMissingParsedData() {
        Task task = taskWith(Map.of("requestId", "req-004"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("completed", result.getOutputData().get("processingStatus"));
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingRequestId() {
        Task task = taskWith(Map.of(
                "parsedData", Map.of("records", 50)));
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
        assertEquals("completed", result.getOutputData().get("processingStatus"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void handlesLargeRecordCount() {
        Task task = taskWith(Map.of(
                "requestId", "req-005",
                "parsedData", Map.of("records", 10000)));
        TaskResult result = worker.execute(task);

        Map<String, Object> processingResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(10000, processingResult.get("recordsProcessed"));
        assertEquals(9998, processingResult.get("recordsSucceeded"));
        assertEquals(2, processingResult.get("recordsFailed"));
    }

    @Test
    void handlesNullRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", null);
        input.put("parsedData", Map.of("records", 10));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("requestId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
