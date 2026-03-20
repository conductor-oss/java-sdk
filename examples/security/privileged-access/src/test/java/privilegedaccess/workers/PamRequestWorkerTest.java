package privilegedaccess.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PamRequestWorkerTest {

    private final PamRequestWorker worker = new PamRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("pam_request", worker.getTaskDefName());
    }

    @Test
    void completesWithRequestId() {
        Task task = taskWith(Map.of(
                "userId", "engineer-01",
                "resource", "production-database",
                "justification", "INC-2024-042 investigation",
                "duration", "2h"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("REQUEST-1391", result.getOutputData().get("requestId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsSuccess() {
        Task task = taskWith(Map.of(
                "userId", "admin-01",
                "resource", "staging-db",
                "justification", "routine check",
                "duration", "1h"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", null);
        input.put("resource", "prod-db");
        input.put("justification", "test");
        input.put("duration", "1h");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullResource() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "user-01");
        input.put("resource", null);
        input.put("justification", "test");
        input.put("duration", "1h");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullJustification() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "user-01");
        input.put("resource", "db");
        input.put("justification", null);
        input.put("duration", "1h");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("requestId"));
    }

    @Test
    void requestIdIsDeterministic() {
        Task task = taskWith(Map.of("userId", "dev-01", "resource", "api", "justification", "debug", "duration", "30m"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("requestId"), r2.getOutputData().get("requestId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
