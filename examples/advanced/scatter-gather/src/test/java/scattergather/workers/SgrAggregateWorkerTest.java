package scattergather.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SgrAggregateWorkerTest {

    private final SgrAggregateWorker worker = new SgrAggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("sgr_aggregate", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void findsLowestPrice() {
        Task task = taskWith(Map.of(
                "response1", Map.of("source", "A", "price", 29.99, "currency", "USD"),
                "response2", Map.of("source", "B", "price", 24.50, "currency", "USD"),
                "response3", Map.of("source", "C", "price", 27.00, "currency", "USD")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> best = (Map<String, Object>) result.getOutputData().get("bestPrice");
        assertEquals("B", best.get("source"));
        assertEquals(24.50, best.get("price"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesAllResponses() {
        Task task = taskWith(Map.of(
                "response1", Map.of("source", "A", "price", 10.0),
                "response2", Map.of("source", "B", "price", 20.0),
                "response3", Map.of("source", "C", "price", 30.0)));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> agg = (List<Map<String, Object>>) result.getOutputData().get("aggregated");
        assertEquals(3, agg.size());
        assertEquals(3, result.getOutputData().get("responseCount"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("bestPrice"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
