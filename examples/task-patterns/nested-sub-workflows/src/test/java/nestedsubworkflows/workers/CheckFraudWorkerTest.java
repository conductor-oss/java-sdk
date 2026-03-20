package nestedsubworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckFraudWorkerTest {

    private final CheckFraudWorker worker = new CheckFraudWorker();

    @Test
    void taskDefName() {
        assertEquals("nest_check_fraud", worker.getTaskDefName());
    }

    @Test
    void returnsDeterministicRiskScore() {
        Task task = taskWith(Map.of("email", "customer@example.com", "amount", 149.99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("riskScore"));
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void riskScoreIsAlways25() {
        // Run multiple times to confirm determinism
        for (int i = 0; i < 5; i++) {
            Task task = taskWith(Map.of("email", "test" + i + "@example.com", "amount", i * 100.0));
            TaskResult result = worker.execute(task);
            assertEquals(25, result.getOutputData().get("riskScore"));
        }
    }

    @Test
    void approvedWhenRiskScoreBelow80() {
        Task task = taskWith(Map.of("email", "safe@example.com", "amount", 50.0));
        TaskResult result = worker.execute(task);

        // Risk score is 25, which is below 80, so approved = true
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesDefaultEmail() {
        Task task = taskWith(Map.of("amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("riskScore"));
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesBlankEmail() {
        Map<String, Object> input = new HashMap<>();
        input.put("email", "   ");
        input.put("amount", 100.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullEmail() {
        Map<String, Object> input = new HashMap<>();
        input.put("email", null);
        input.put("amount", 75.0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("riskScore"));
    }

    @Test
    void handlesMissingAmount() {
        Task task = taskWith(Map.of("email", "customer@example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("riskScore"));
    }

    @Test
    void handlesIntegerAmount() {
        Task task = taskWith(Map.of("email", "customer@example.com", "amount", 200));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(25, result.getOutputData().get("riskScore"));
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith(Map.of("email", "customer@example.com", "amount", 100.0));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
        assertTrue(result.getOutputData().containsKey("riskScore"));
        assertTrue(result.getOutputData().containsKey("approved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
