package requestaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchOrdersWorkerTest {

    private final FetchOrdersWorker worker = new FetchOrdersWorker();

    @Test
    void taskDefName() { assertEquals("agg_fetch_orders", worker.getTaskDefName()); }

    @Test
    void fetchesOrders() {
        Task task = taskWith(Map.of("userId", "user-42"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("orders"));
    }

    @Test
    void returnsTwoOrders() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) result.getOutputData().get("orders");
        assertEquals(2, orders.size());
    }

    @Test
    void firstOrderHasId() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) result.getOutputData().get("orders");
        assertEquals("ORD-1", orders.get(0).get("id"));
    }

    @Test
    void handlesNullUserId() {
        Map<String, Object> input = new HashMap<>(); input.put("userId", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void isDeterministic() {
        TaskResult r1 = worker.execute(taskWith(Map.of("userId", "a")));
        TaskResult r2 = worker.execute(taskWith(Map.of("userId", "b")));
        assertEquals(r1.getOutputData().get("orders"), r2.getOutputData().get("orders"));
    }

    @Test
    void ordersContainTotals() {
        TaskResult result = worker.execute(taskWith(Map.of("userId", "u1")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) result.getOutputData().get("orders");
        assertNotNull(orders.get(0).get("total"));
        assertNotNull(orders.get(1).get("total"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input)); return task;
    }
}
