package loadbalancing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateResultsWorkerTest {

    private final AggregateResultsWorker worker = new AggregateResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("lb_aggregate_results", worker.getTaskDefName());
    }

    @Test
    void aggregatesResults() {
        Map<String, Object> r1 = Map.of("instanceId", "inst-1", "recordsProcessed", 50, "latencyMs", 40);
        Map<String, Object> r2 = Map.of("instanceId", "inst-2", "recordsProcessed", 50, "latencyMs", 50);
        Map<String, Object> r3 = Map.of("instanceId", "inst-3", "recordsProcessed", 50, "latencyMs", 45);
        Task task = taskWith(Map.of("result1", r1, "result2", r2, "result3", r3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(150, result.getOutputData().get("totalProcessed"));
        assertEquals(45, result.getOutputData().get("avgLatency"));
    }

    @Test
    void returnsTotalProcessed() {
        Map<String, Object> r1 = Map.of("recordsProcessed", 30, "latencyMs", 20);
        Map<String, Object> r2 = Map.of("recordsProcessed", 40, "latencyMs", 30);
        Map<String, Object> r3 = Map.of("recordsProcessed", 50, "latencyMs", 40);
        Task task = taskWith(Map.of("result1", r1, "result2", r2, "result3", r3));
        TaskResult result = worker.execute(task);

        assertEquals(120, result.getOutputData().get("totalProcessed"));
    }

    @Test
    void returnsAggregatedList() {
        Map<String, Object> r1 = Map.of("recordsProcessed", 50, "latencyMs", 45);
        Map<String, Object> r2 = Map.of("recordsProcessed", 50, "latencyMs", 45);
        Map<String, Object> r3 = Map.of("recordsProcessed", 50, "latencyMs", 45);
        Task task = taskWith(Map.of("result1", r1, "result2", r2, "result3", r3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agg = (List<Map<String, Object>>) result.getOutputData().get("aggregated");
        assertEquals(3, agg.size());
    }

    @Test
    void handlesNullResult1() {
        Map<String, Object> input = new HashMap<>();
        input.put("result1", null);
        input.put("result2", Map.of("recordsProcessed", 50, "latencyMs", 45));
        input.put("result3", Map.of("recordsProcessed", 50, "latencyMs", 45));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(100, result.getOutputData().get("totalProcessed"));
    }

    @Test
    void handlesAllNullResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("result1", null);
        input.put("result2", null);
        input.put("result3", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalProcessed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Map<String, Object> r = Map.of("recordsProcessed", 50, "latencyMs", 45);
        Task task = taskWith(Map.of("result1", r, "result2", r, "result3", r));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("aggregated"));
        assertTrue(result.getOutputData().containsKey("totalProcessed"));
        assertTrue(result.getOutputData().containsKey("avgLatency"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
