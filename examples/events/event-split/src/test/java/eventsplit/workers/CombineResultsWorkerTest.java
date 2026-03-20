package eventsplit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombineResultsWorkerTest {

    private final CombineResultsWorker worker = new CombineResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("sp_combine_results", worker.getTaskDefName());
    }

    @Test
    void combinesThreeResults() {
        Task task = taskWith(Map.of(
                "resultA", "order_validated",
                "resultB", "customer_verified",
                "resultC", "shipping_calculated"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_sub_events_processed", result.getOutputData().get("status"));
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertEquals(3, results.size());
        assertEquals("order_validated", results.get(0));
        assertEquals("customer_verified", results.get(1));
        assertEquals("shipping_calculated", results.get(2));
    }

    @Test
    void statusIsAlwaysAllSubEventsProcessed() {
        Task task = taskWith(Map.of(
                "resultA", "a",
                "resultB", "b",
                "resultC", "c"));
        TaskResult result = worker.execute(task);

        assertEquals("all_sub_events_processed", result.getOutputData().get("status"));
    }

    @Test
    void resultsListContainsAllThreeInputs() {
        Task task = taskWith(Map.of(
                "resultA", "first",
                "resultB", "second",
                "resultC", "third"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertTrue(results.contains("first"));
        assertTrue(results.contains("second"));
        assertTrue(results.contains("third"));
    }

    @Test
    void handlesNullResultA() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", null);
        input.put("resultB", "customer_verified");
        input.put("resultC", "shipping_calculated");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertEquals("unknown", results.get(0));
        assertEquals("customer_verified", results.get(1));
    }

    @Test
    void handlesNullResultB() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", "order_validated");
        input.put("resultB", null);
        input.put("resultC", "shipping_calculated");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertEquals("unknown", results.get(1));
    }

    @Test
    void handlesNullResultC() {
        Map<String, Object> input = new HashMap<>();
        input.put("resultA", "order_validated");
        input.put("resultB", "customer_verified");
        input.put("resultC", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertEquals("unknown", results.get(2));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_sub_events_processed", result.getOutputData().get("status"));
        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) result.getOutputData().get("results");
        assertEquals(3, results.size());
        assertEquals("unknown", results.get(0));
        assertEquals("unknown", results.get(1));
        assertEquals("unknown", results.get(2));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
