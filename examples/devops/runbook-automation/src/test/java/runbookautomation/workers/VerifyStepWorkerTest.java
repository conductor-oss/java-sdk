package runbookautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyStepWorkerTest {

    private final VerifyStepWorker worker = new VerifyStepWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_verify_step", worker.getTaskDefName());
    }

    @Test
    void verifiesSuccessfulExecution() {
        Map<String, Object> stepResult = new HashMap<>();
        stepResult.put("executed", true);
        stepResult.put("successCount", 3);
        stepResult.put("failureCount", 0);
        stepResult.put("totalDurationMs", 500L);
        stepResult.put("results", List.of(
                Map.of("name", "step1", "exitCode", 0, "timedOut", false),
                Map.of("name", "step2", "exitCode", 0, "timedOut", false),
                Map.of("name", "step3", "exitCode", 0, "timedOut", false)
        ));

        Task task = taskWith(Map.of("stepResult", stepResult));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test
    void detectsFailedSteps() {
        Map<String, Object> stepResult = new HashMap<>();
        stepResult.put("successCount", 1);
        stepResult.put("failureCount", 1);
        stepResult.put("totalDurationMs", 300L);
        stepResult.put("results", List.of(
                Map.of("name", "step1", "exitCode", 0, "timedOut", false),
                Map.of("name", "step2", "exitCode", 1, "timedOut", false)
        ));

        Task task = taskWith(Map.of("stepResult", stepResult));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void detectsTimeouts() {
        Map<String, Object> stepResult = new HashMap<>();
        stepResult.put("successCount", 0);
        stepResult.put("failureCount", 1);
        stepResult.put("totalDurationMs", 30000L);
        stepResult.put("results", List.of(
                Map.of("name", "slow-step", "exitCode", -1, "timedOut", true)
        ));

        Task task = taskWith(Map.of("stepResult", stepResult));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void runsRealSystemHealthCheck() {
        // Even with empty step results, should run real system health check
        Task task = taskWith(Map.of("stepResult", Map.of("successCount", 1, "failureCount", 0,
                "totalDurationMs", 100L, "results", List.of())));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertTrue(checks.size() >= 3, "Should have at least 3 checks");

        // The system-health check should have been run
        assertTrue(checks.stream().anyMatch(c -> "system-health".equals(c.get("check"))));
    }

    @Test
    void outputContainsCheckCounts() {
        Task task = taskWith(Map.of("stepResult", Map.of("successCount", 0, "failureCount", 0,
                "totalDurationMs", 0L, "results", List.of())));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("checksTotal"));
        assertNotNull(result.getOutputData().get("checksPassed"));
        assertNotNull(result.getOutputData().get("verifiedAt"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
