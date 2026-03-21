package eventttl.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogExpiredWorkerTest {

    private final LogExpiredWorker worker = new LogExpiredWorker();

    @Test
    void taskDefName() {
        assertEquals("xl_log_expired", worker.getTaskDefName());
    }

    @Test
    void logsExpiredEventSuccessfully() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "age", 700,
                "ttl", 600));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("TTL exceeded", result.getOutputData().get("reason"));
    }

    @Test
    void outputContainsLoggedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "age", 500,
                "ttl", 300));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("logged"));
    }

    @Test
    void outputContainsReasonTtlExceeded() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "age", 1000,
                "ttl", 600));
        TaskResult result = worker.execute(task);

        assertEquals("TTL exceeded", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("age", 800);
        input.put("ttl", 600);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesMissingAgeAndTtl() {
        Task task = taskWith(Map.of("eventId", "evt-005"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("TTL exceeded", result.getOutputData().get("reason"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("TTL exceeded", result.getOutputData().get("reason"));
    }

    @Test
    void handlesStringAgeAndTtl() {
        Task task = taskWith(Map.of(
                "eventId", "evt-007",
                "age", "900",
                "ttl", "600"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
