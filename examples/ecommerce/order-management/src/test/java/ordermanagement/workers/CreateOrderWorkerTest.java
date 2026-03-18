package ordermanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderWorkerTest {

    private CreateOrderWorker worker;

    @BeforeEach
    void setUp() {
        OrderStore.reset();
        worker = new CreateOrderWorker();
    }

    @Test
    void taskDefName() { assertEquals("ord_create", worker.getTaskDefName()); }

    @Test
    void createsOrderId() {
        Task task = taskWith(Map.of("customerId", "c1", "items", List.of(
                Map.of("sku", "A", "name", "Item A", "price", 10.0, "qty", 2))));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("orderId").toString().startsWith("ORD-"));
    }

    @Test
    void calculatesTotal() {
        Task task = taskWith(Map.of("customerId", "c2", "items", List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 2),
                Map.of("sku", "B", "name", "B", "price", 5.0, "qty", 3))));
        TaskResult r = worker.execute(task);
        assertEquals("35.00", r.getOutputData().get("total"));
    }

    @Test
    void orderIdIsUnique() {
        Task t1 = taskWith(Map.of("customerId", "c1", "items", List.of(
                Map.of("sku", "X", "name", "X", "price", 1.0, "qty", 1))));
        Task t2 = taskWith(Map.of("customerId", "c2", "items", List.of(
                Map.of("sku", "Y", "name", "Y", "price", 1.0, "qty", 1))));
        assertNotEquals(worker.execute(t1).getOutputData().get("orderId"),
                worker.execute(t2).getOutputData().get("orderId"));
    }

    @Test
    void failsWithEmptyItems() {
        Task task = taskWith(Map.of("customerId", "c3", "items", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test
    void setsInitialStatusToCreated() {
        Task task = taskWith(Map.of("customerId", "c4", "items", List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = worker.execute(task);
        assertEquals("CREATED", r.getOutputData().get("status"));
    }

    @Test
    void storesOrderInOrderStore() {
        Task task = taskWith(Map.of("customerId", "c5", "items", List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = worker.execute(task);
        String orderId = r.getOutputData().get("orderId").toString();

        Map<String, Object> stored = OrderStore.get(orderId);
        assertNotNull(stored);
        assertEquals("CREATED", stored.get("status"));
        assertEquals("c5", stored.get("customerId"));
    }

    @Test
    void rejectsItemsWithZeroPrice() {
        Task task = taskWith(Map.of("customerId", "c6", "items", List.of(
                Map.of("sku", "BAD", "name", "Bad", "price", 0.0, "qty", 1))));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test
    void rejectsItemsWithZeroQuantity() {
        Task task = taskWith(Map.of("customerId", "c7", "items", List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 0))));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test
    void includesCreatedAt() {
        Task task = taskWith(Map.of("customerId", "c8", "items", List.of(
                Map.of("sku", "A", "name", "A", "price", 10.0, "qty", 1))));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("createdAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
