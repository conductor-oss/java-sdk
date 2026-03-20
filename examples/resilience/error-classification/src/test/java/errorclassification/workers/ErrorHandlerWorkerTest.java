package errorclassification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerWorkerTest {

    @Test
    void taskDefName() {
        ErrorHandlerWorker worker = new ErrorHandlerWorker();
        assertEquals("ec_handle_error", worker.getTaskDefName());
    }

    @Test
    void handlesNonRetryableError() {
        ErrorHandlerWorker worker = new ErrorHandlerWorker();
        Task task = taskWith(Map.of(
                "errorType", "non_retryable",
                "error", "Bad Request: invalid input parameters",
                "httpStatus", 400
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("non_retryable", result.getOutputData().get("errorType"));
        assertEquals("Bad Request: invalid input parameters", result.getOutputData().get("error"));
        assertEquals("400", result.getOutputData().get("httpStatus"));
    }

    @Test
    void handlesErrorWithMissingFields() {
        ErrorHandlerWorker worker = new ErrorHandlerWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("unknown", result.getOutputData().get("errorType"));
        assertEquals("no details", result.getOutputData().get("error"));
        assertEquals("N/A", result.getOutputData().get("httpStatus"));
    }

    @Test
    void outputContainsAllFields() {
        ErrorHandlerWorker worker = new ErrorHandlerWorker();
        Task task = taskWith(Map.of(
                "errorType", "non_retryable",
                "error", "some error",
                "httpStatus", 400
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("handled"));
        assertTrue(result.getOutputData().containsKey("errorType"));
        assertTrue(result.getOutputData().containsKey("error"));
        assertTrue(result.getOutputData().containsKey("httpStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
