package creditscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculateFactorsWorkerTest {

    private final CalculateFactorsWorker worker = new CalculateFactorsWorker();

    @Test
    void taskDefName() {
        assertEquals("csc_calculate_factors", worker.getTaskDefName());
    }

    @Test
    void returnsFactors() {
        Task task = taskWith(Map.of("applicantId", "APP-001",
                "creditHistory", Map.of("totalAccounts", 8, "utilization", 28)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("factors"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void factorsContainPaymentHistory() {
        Task task = taskWith(Map.of("applicantId", "APP-002",
                "creditHistory", Map.of("totalAccounts", 8, "utilization", 28)));
        TaskResult result = worker.execute(task);

        Map<String, Object> factors = (Map<String, Object>) result.getOutputData().get("factors");
        assertNotNull(factors.get("paymentHistory"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void paymentHistoryWeight35() {
        Task task = taskWith(Map.of("applicantId", "APP-003",
                "creditHistory", Map.of("totalAccounts", 8, "utilization", 28)));
        TaskResult result = worker.execute(task);

        Map<String, Object> factors = (Map<String, Object>) result.getOutputData().get("factors");
        Map<String, Object> ph = (Map<String, Object>) factors.get("paymentHistory");
        assertEquals(35, ph.get("weight"));
        assertEquals(92, ph.get("score"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void utilizationWeight30() {
        Task task = taskWith(Map.of("applicantId", "APP-004",
                "creditHistory", Map.of("totalAccounts", 8, "utilization", 28)));
        TaskResult result = worker.execute(task);

        Map<String, Object> factors = (Map<String, Object>) result.getOutputData().get("factors");
        Map<String, Object> u = (Map<String, Object>) factors.get("utilization");
        assertEquals(30, u.get("weight"));
        assertEquals(85, u.get("score"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void factorsContainFiveCategories() {
        Task task = taskWith(Map.of("applicantId", "APP-005",
                "creditHistory", Map.of("totalAccounts", 8, "utilization", 28)));
        TaskResult result = worker.execute(task);

        Map<String, Object> factors = (Map<String, Object>) result.getOutputData().get("factors");
        assertEquals(5, factors.size());
    }

    @Test
    void handlesNullCreditHistory() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicantId", "APP-006");
        input.put("creditHistory", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("factors"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
