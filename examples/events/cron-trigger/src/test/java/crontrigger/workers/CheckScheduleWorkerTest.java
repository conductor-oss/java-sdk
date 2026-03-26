package crontrigger.workers;

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
        assertEquals("cn_check_schedule", worker.getTaskDefName());
    }

    @Test
    void returnsShouldRunYes() {
        Task task = taskWith(Map.of("cronExpression", "0 */5 * * *", "jobName", "daily-report"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("yes", result.getOutputData().get("shouldRun"));
    }

    @Test
    void returnsScheduledTime() {
        Task task = taskWith(Map.of("cronExpression", "0 0 * * *", "jobName", "nightly-backup"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("scheduledTime"));
    }

    @Test
    void returnsCronExpression() {
        Task task = taskWith(Map.of("cronExpression", "*/10 * * * *", "jobName", "health-check"));
        TaskResult result = worker.execute(task);

        assertEquals("*/10 * * * *", result.getOutputData().get("cronExpression"));
    }

    @Test
    void handlesNullCronExpression() {
        Map<String, Object> input = new HashMap<>();
        input.put("cronExpression", null);
        input.put("jobName", "some-job");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("cronExpression"));
        assertEquals("yes", result.getOutputData().get("shouldRun"));
    }

    @Test
    void handlesNullJobName() {
        Map<String, Object> input = new HashMap<>();
        input.put("cronExpression", "0 0 * * *");
        input.put("jobName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("yes", result.getOutputData().get("shouldRun"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("yes", result.getOutputData().get("shouldRun"));
        assertEquals("unknown", result.getOutputData().get("cronExpression"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("cronExpression", "0 */5 * * *", "jobName", "report-gen"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("shouldRun"));
        assertTrue(result.getOutputData().containsKey("scheduledTime"));
        assertTrue(result.getOutputData().containsKey("cronExpression"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
