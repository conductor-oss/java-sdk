package eventchoreography.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceWorkerTest {

    private final InventoryServiceWorker worker = new InventoryServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("ch_inventory_service", worker.getTaskDefName());
    }

    @Test
    void reservesInventory() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "items", List.of(Map.of("sku", "ITEM-A"), Map.of("sku", "ITEM-B"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reserved"));
        assertEquals(2, result.getOutputData().get("itemCount"));
    }

    @Test
    void outputContainsReservedTrue() {
        Task task = taskWith(Map.of("orderId", "ORD-200", "items", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("reserved"));
    }

    @Test
    void outputContainsItemCount() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "items", List.of(Map.of("sku", "X"))));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("itemCount"));
    }

    @Test
    void handlesNullItems() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-400");
        input.put("items", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reserved"));
    }

    @Test
    void handlesNullOrderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", null);
        input.put("items", List.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reserved"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reserved"));
        assertEquals(2, result.getOutputData().get("itemCount"));
    }

    @Test
    void itemCountIsDeterministic() {
        Task task1 = taskWith(Map.of("orderId", "ORD-A", "items", List.of(Map.of("sku", "X"))));
        Task task2 = taskWith(Map.of("orderId", "ORD-B", "items", List.of(Map.of("sku", "Y"), Map.of("sku", "Z"))));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("itemCount"),
                     result2.getOutputData().get("itemCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
