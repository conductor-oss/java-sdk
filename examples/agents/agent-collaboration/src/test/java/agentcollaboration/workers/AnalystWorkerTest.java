package agentcollaboration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalystWorkerTest {

    private final AnalystWorker worker = new AnalystWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_analyst", worker.getTaskDefName());
    }

    @Test
    void returnsFourInsights() {
        Task task = taskWith(Map.of("businessContext", "High churn rate"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> insights =
                (List<Map<String, Object>>) result.getOutputData().get("insights");
        assertNotNull(insights);
        assertEquals(4, insights.size());
    }

    @Test
    void insightsHaveRequiredFields() {
        Task task = taskWith(Map.of("businessContext", "Declining sales"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> insights =
                (List<Map<String, Object>>) result.getOutputData().get("insights");

        for (Map<String, Object> insight : insights) {
            assertTrue(insight.containsKey("id"), "missing id");
            assertTrue(insight.containsKey("finding"), "missing finding");
            assertTrue(insight.containsKey("severity"), "missing severity");
            assertTrue(insight.containsKey("category"), "missing category");
        }
    }

    @Test
    void insightIdsAreUnique() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> insights =
                (List<Map<String, Object>>) result.getOutputData().get("insights");

        long distinctIds = insights.stream()
                .map(i -> (String) i.get("id"))
                .distinct()
                .count();
        assertEquals(4, distinctIds);
    }

    @Test
    void returnsMetricsMap() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> metrics =
                (Map<String, Object>) result.getOutputData().get("metrics");
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("churnRate"));
        assertTrue(metrics.containsKey("repeatPurchaseDecline"));
        assertTrue(metrics.containsKey("avgSupportResponseHours"));
        assertTrue(metrics.containsKey("loyaltyEngagementRate"));
    }

    @Test
    void returnsInsightCount() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("insightCount"));
    }

    @Test
    void handlesNullBusinessContext() {
        Map<String, Object> input = new HashMap<>();
        input.put("businessContext", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("insights"));
    }

    @Test
    void handlesMissingBusinessContext() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("insights"));
    }

    @Test
    void handlesBlankBusinessContext() {
        Task task = taskWith(Map.of("businessContext", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("insights"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
