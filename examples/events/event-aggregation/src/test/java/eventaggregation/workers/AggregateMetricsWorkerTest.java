package eventaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMetricsWorkerTest {

    private final AggregateMetricsWorker worker = new AggregateMetricsWorker();

    @Test
    void taskDefName() {
        assertEquals("eg_aggregate_metrics", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aggregatesWithFixedValues() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", 6));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        assertNotNull(aggregation);
        assertEquals(6, aggregation.get("totalEvents"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsTotalRevenue() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", 6));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        assertEquals(478.47, aggregation.get("totalRevenue"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsAvgOrderValue() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", 6));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        assertEquals(100.70, aggregation.get("avgOrderValue"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsPurchaseAndRefundCounts() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", 6));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        assertEquals(5, aggregation.get("purchaseCount"));
        assertEquals(1, aggregation.get("refundCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsProductBreakdown() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", 6));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        Map<String, Integer> breakdown =
                (Map<String, Integer>) aggregation.get("productBreakdown");
        assertNotNull(breakdown);
        assertEquals(3, breakdown.get("Widget A"));
        assertEquals(2, breakdown.get("Widget B"));
        assertEquals(1, breakdown.get("Widget C"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregation"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("eventCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> aggregation =
                (Map<String, Object>) result.getOutputData().get("aggregation");
        assertNotNull(aggregation);
        assertEquals(478.47, aggregation.get("totalRevenue"));
    }

    @Test
    void handlesStringEventCount() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "eventCount", "6"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregation"));
    }

    private List<Map<String, Object>> sampleEvents() {
        return List.of(
                Map.of("id", "e1", "type", "purchase", "amount", 49.99, "product", "Widget A"),
                Map.of("id", "e2", "type", "purchase", "amount", 129.00, "product", "Widget B"),
                Map.of("id", "e3", "type", "refund", "amount", -25.00, "product", "Widget A"),
                Map.of("id", "e4", "type", "purchase", "amount", 89.50, "product", "Widget C"),
                Map.of("id", "e5", "type", "purchase", "amount", 199.99, "product", "Widget B"),
                Map.of("id", "e6", "type", "purchase", "amount", 34.99, "product", "Widget A")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
