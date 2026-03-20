package crontrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogSkipWorkerTest {

    private final LogSkipWorker worker = new LogSkipWorker();

    @Test
    void taskDefName() {
        assertEquals("cn_log_skip", worker.getTaskDefName());
    }

    @Test
    void returnsSkippedTrue() {
        Task task = taskWith(Map.of("jobName", "daily-report", "reason", "outside schedule window"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
    }

    @Test
    void returnsReasonFromInput() {
        Task task = taskWith(Map.of("jobName", "nightly-backup", "reason", "maintenance mode"));
        TaskResult result = worker.execute(task);

        assertEquals("maintenance mode", result.getOutputData().get("reason"));
    }

    @Test
    void handlesDifferentReasons() {
        Task task = taskWith(Map.of("jobName", "health-check", "reason", "job disabled"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertEquals("job disabled", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullJobName() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", null);
        input.put("reason", "not scheduled");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
    }

    @Test
    void handlesNullReason() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", "some-job");
        input.put("reason", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertEquals("no reason provided", result.getOutputData().get("reason"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("skipped"));
        assertEquals("no reason provided", result.getOutputData().get("reason"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("jobName", "report-gen", "reason", "off hours"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("skipped"));
        assertTrue(result.getOutputData().containsKey("reason"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
