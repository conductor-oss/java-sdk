package crontrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteTasksWorkerTest {

    private final ExecuteTasksWorker worker = new ExecuteTasksWorker();

    @Test
    void taskDefName() {
        assertEquals("cn_execute_tasks", worker.getTaskDefName());
    }

    @Test
    void returnsSuccessResult() {
        Task task = taskWith(Map.of("jobName", "daily-report", "scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void returnsExecutedAt() {
        Task task = taskWith(Map.of("jobName", "nightly-backup", "scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:01Z", result.getOutputData().get("executedAt"));
    }

    @Test
    void returnsDuration() {
        Task task = taskWith(Map.of("jobName", "health-check", "scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(1250, result.getOutputData().get("duration"));
    }

    @Test
    void handlesNullJobName() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", null);
        input.put("scheduledTime", "2026-01-15T10:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullScheduledTime() {
        Map<String, Object> input = new HashMap<>();
        input.put("jobName", "some-job");
        input.put("scheduledTime", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("result"));
        assertEquals("2026-01-15T10:00:01Z", result.getOutputData().get("executedAt"));
        assertEquals(1250, result.getOutputData().get("duration"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("jobName", "report-gen", "scheduledTime", "2026-01-15T10:00:00Z"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("executedAt"));
        assertTrue(result.getOutputData().containsKey("duration"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
