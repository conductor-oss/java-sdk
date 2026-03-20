package simpleplussystem.workers;

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
    void taskDefName() {
        assertEquals("fetch_orders", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("storeId", "STORE-1", "dateRange", "2026-Q1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsOrdersList() {
        Task task = taskWith(Map.of("storeId", "STORE-42", "dateRange", "2026-Q1"));
        TaskResult result = worker.execute(task);

        Object orders = result.getOutputData().get("orders");
        assertNotNull(orders);
        assertInstanceOf(List.class, orders);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderList = (List<Map<String, Object>>) orders;
        assertEquals(5, orderList.size());
    }

    @Test
    void eachOrderHasRequiredFields() {
        Task task = taskWith(Map.of("storeId", "STORE-42"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) result.getOutputData().get("orders");

        for (Map<String, Object> order : orders) {
            assertTrue(order.containsKey("id"), "Order should have an id");
            assertTrue(order.containsKey("amount"), "Order should have an amount");
            assertTrue(order.containsKey("items"), "Order should have items count");
        }
    }

    @Test
    void orderAmountsArePositive() {
        Task task = taskWith(Map.of("storeId", "STORE-1"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) result.getOutputData().get("orders");

        for (Map<String, Object> order : orders) {
            int amount = ((Number) order.get("amount")).intValue();
            assertTrue(amount > 0, "Order amount should be positive");
        }
    }

    @Test
    void handlesNullDateRange() {
        Map<String, Object> input = new HashMap<>();
        input.put("storeId", "STORE-1");
        input.put("dateRange", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("orders"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
