package errorclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiCallWorkerTest {

    @Test
    void taskDefName() {
        ApiCallWorker worker = new ApiCallWorker();
        assertEquals("ec_api_call", worker.getTaskDefName());
    }

    @Test
    void returns400NonRetryableError() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "security-posture"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("non_retryable", result.getOutputData().get("errorType"));
        assertEquals(400, result.getOutputData().get("httpStatus"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void returns429RetryableError() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "429"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(429, result.getOutputData().get("httpStatus"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void returns503RetryableError() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "503"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(503, result.getOutputData().get("httpStatus"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void returnsSuccessWhenNoError() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
        assertEquals("none", result.getOutputData().get("errorType"));
    }

    @Test
    void returnsSuccessWhenSimulateErrorMissing() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
        assertEquals("none", result.getOutputData().get("errorType"));
    }

    @Test
    void nonRetryableErrorContainsErrorMessage() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "security-posture"));
        TaskResult result = worker.execute(task);

        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("Bad Request"));
    }

    @Test
    void retryable429ContainsErrorMessage() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "429"));
        TaskResult result = worker.execute(task);

        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("Too Many Requests"));
    }

    @Test
    void retryable503ContainsErrorMessage() {
        ApiCallWorker worker = new ApiCallWorker();
        Task task = taskWith(Map.of("triggerError", "503"));
        TaskResult result = worker.execute(task);

        String error = (String) result.getOutputData().get("error");
        assertTrue(error.contains("Service Unavailable"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
