package eventaudittrail.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogValidatedWorkerTest {

    private final LogValidatedWorker worker = new LogValidatedWorker();

    @Test
    void taskDefName() {
        assertEquals("at_log_validated", worker.getTaskDefName());
    }

    @Test
    void logsValidatedEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "validationResult", true,
                "stage", "validated"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("validated", result.getOutputData().get("stage"));
        assertEquals("2026-01-15T10:00:01Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void outputContainsLoggedTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "validationResult", true,
                "stage", "validated"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void outputStageIsAlwaysValidated() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "validationResult", false,
                "stage", "some-other-stage"));
        TaskResult result = worker.execute(task);

        assertEquals("validated", result.getOutputData().get("stage"));
    }

    @Test
    void outputContainsFixedTimestamp() {
        Task task = taskWith(Map.of(
                "eventId", "evt-004",
                "validationResult", true,
                "stage", "validated"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:01Z", result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("validationResult", true);
        input.put("stage", "validated");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void handlesNullValidationResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-005");
        input.put("validationResult", null);
        input.put("stage", "validated");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("validated", result.getOutputData().get("stage"));
    }

    @Test
    void handlesNullStage() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", "evt-006");
        input.put("validationResult", true);
        input.put("stage", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("validated", result.getOutputData().get("stage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
