package crmagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckHistoryWorkerTest {

    private final CheckHistoryWorker worker = new CheckHistoryWorker();

    @Test
    void taskDefName() {
        assertEquals("cm_check_history", worker.getTaskDefName());
    }

    @Test
    void returnsHistoryData() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "customerTier", "enterprise", "accountAge", "3 years"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(47, result.getOutputData().get("totalInteractions"));
        assertEquals(4.2, result.getOutputData().get("avgSatisfaction"));
        assertEquals("positive", result.getOutputData().get("sentiment"));
        assertEquals("2026-02-28", result.getOutputData().get("lastContact"));
        assertEquals(1, result.getOutputData().get("openTickets"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsThreeRecentIssues() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "customerTier", "enterprise", "accountAge", "3 years"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> issues =
                (List<Map<String, Object>>) result.getOutputData().get("recentIssues");
        assertNotNull(issues);
        assertEquals(3, issues.size());

        Map<String, Object> firstIssue = issues.get(0);
        assertEquals("2026-02-15", firstIssue.get("date"));
        assertEquals("technical", firstIssue.get("type"));
        assertEquals("API rate limiting errors", firstIssue.get("subject"));
        assertEquals("resolved", firstIssue.get("status"));
        assertEquals(5, firstIssue.get("satisfaction"));
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("customerTier", "enterprise");
        input.put("accountAge", "3 years");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("totalInteractions"));
    }

    @Test
    void handlesNullTier() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "CUST-4821");
        input.put("customerTier", null);
        input.put("accountAge", "3 years");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sentiment"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("recentIssues"));
        assertNotNull(result.getOutputData().get("totalInteractions"));
    }

    @Test
    void handlesBlankAccountAge() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "customerTier", "standard", "accountAge", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("lastContact"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("customerId", "CUST-4821",
                "customerTier", "enterprise", "accountAge", "3 years"));
        TaskResult result = worker.execute(task);

        Map<String, Object> output = result.getOutputData();
        assertEquals(6, output.size());
        assertTrue(output.containsKey("recentIssues"));
        assertTrue(output.containsKey("totalInteractions"));
        assertTrue(output.containsKey("avgSatisfaction"));
        assertTrue(output.containsKey("sentiment"));
        assertTrue(output.containsKey("lastContact"));
        assertTrue(output.containsKey("openTickets"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
