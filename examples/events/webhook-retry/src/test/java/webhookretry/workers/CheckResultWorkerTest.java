package webhookretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckResultWorkerTest {

    private final CheckResultWorker worker = new CheckResultWorker();

    @Test
    void taskDefName() {
        assertEquals("wr_check_result", worker.getTaskDefName());
    }

    @Test
    void successWhenStatusCode200() {
        Task task = taskWith(Map.of("statusCode", 200, "attempt", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    @Test
    void failureWhenStatusCode503() {
        Task task = taskWith(Map.of("statusCode", 503, "attempt", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("success"));
        assertEquals(503, result.getOutputData().get("statusCode"));
    }

    @Test
    void failureWhenStatusCode500() {
        Task task = taskWith(Map.of("statusCode", 500, "attempt", 2));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("success"));
        assertEquals(500, result.getOutputData().get("statusCode"));
    }

    @Test
    void failureWhenStatusCode404() {
        Task task = taskWith(Map.of("statusCode", 404, "attempt", 1));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("success"));
    }

    @Test
    void handlesMissingStatusCode() {
        Task task = taskWith(Map.of("attempt", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("success"));
        assertEquals(500, result.getOutputData().get("statusCode"));
    }

    @Test
    void outputContainsStatusCode() {
        Task task = taskWith(Map.of("statusCode", 200, "attempt", 3));
        TaskResult result = worker.execute(task);

        assertEquals(200, result.getOutputData().get("statusCode"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("success"));
        assertNotNull(result.getOutputData().get("statusCode"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
