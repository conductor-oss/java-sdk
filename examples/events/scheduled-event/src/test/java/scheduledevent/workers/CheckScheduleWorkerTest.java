package scheduledevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckScheduleWorkerTest {

    private final CheckScheduleWorker worker = new CheckScheduleWorker();

    @Test
    void taskDefName() {
        assertEquals("se_check_schedule", worker.getTaskDefName());
    }

    @Test
    void checksScheduleSuccessfully() {
        Task task = taskWith(Map.of("scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("delayMs"));
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void outputContainsDelayMs() {
        Task task = taskWith(Map.of("scheduledTime", "2026-02-01T08:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("delayMs"));
    }

    @Test
    void outputContainsReadyFlag() {
        Task task = taskWith(Map.of("scheduledTime", "2026-06-15T14:30:00Z"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("ready"));
    }

    @Test
    void handlesNullScheduledTime() {
        Map<String, Object> input = new HashMap<>();
        input.put("scheduledTime", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("delayMs"));
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("delayMs"));
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void handlesIsoTimestamp() {
        Task task = taskWith(Map.of("scheduledTime", "2026-12-31T23:59:59Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("delayMs"));
    }

    @Test
    void handlesEpochTimestamp() {
        Task task = taskWith(Map.of("scheduledTime", "1970-01-01T00:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("ready"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
