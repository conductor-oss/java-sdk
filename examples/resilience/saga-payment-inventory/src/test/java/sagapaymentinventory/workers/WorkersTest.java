package sagapaymentinventory.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkersTest {

    // ---- ReserveInventoryWorker tests ----

    @Test
    void reserveInventory_taskDefName() {
        assertEquals("spi_reserve_inventory", new ReserveInventoryWorker().getTaskDefName());
    }

    @Test
    void reserveInventory_happyPath() {
        ReserveInventoryWorker worker = new ReserveInventoryWorker();
        Task task = taskWith(Map.of("orderId", "ORD-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("INV-001", result.getOutputData().get("reservationId"));
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals("reserved", result.getOutputData().get("inventoryStatus"));
    }

    @Test
    void reserveInventory_missingOrderId() {
        ReserveInventoryWorker worker = new ReserveInventoryWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("INV-001", result.getOutputData().get("reservationId"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("reserved", result.getOutputData().get("inventoryStatus"));
    }

    // ---- ChargePaymentWorker tests ----

    @Test
    void chargePayment_taskDefName() {
        assertEquals("spi_charge_payment", new ChargePaymentWorker().getTaskDefName());
    }

    @Test
    void chargePayment_happyPath() {
        ChargePaymentWorker worker = new ChargePaymentWorker();
        Task task = taskWith(Map.of("orderId", "ORD-100", "amount", 99.99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PAY-001", result.getOutputData().get("paymentId"));
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals(99.99, result.getOutputData().get("amount"));
        assertEquals("charged", result.getOutputData().get("paymentStatus"));
    }

    @Test
    void chargePayment_missingInputs() {
        ChargePaymentWorker worker = new ChargePaymentWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PAY-001", result.getOutputData().get("paymentId"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals(0, result.getOutputData().get("amount"));
        assertEquals("charged", result.getOutputData().get("paymentStatus"));
    }

    @Test
    void chargePayment_integerAmount() {
        ChargePaymentWorker worker = new ChargePaymentWorker();
        Task task = taskWith(Map.of("orderId", "ORD-200", "amount", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(50, result.getOutputData().get("amount"));
    }

    // ---- ShipOrderWorker tests ----

    @Test
    void shipOrder_taskDefName() {
        assertEquals("spi_ship_order", new ShipOrderWorker().getTaskDefName());
    }

    @Test
    void shipOrder_successWhenShouldFailFalse() {
        ShipOrderWorker worker = new ShipOrderWorker();
        Task task = taskWith(Map.of("orderId", "ORD-100", "shouldFail", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-001", result.getOutputData().get("shipmentId"));
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals("shipped", result.getOutputData().get("shipStatus"));
    }

    @Test
    void shipOrder_failsWhenShouldFailTrue() {
        ShipOrderWorker worker = new ShipOrderWorker();
        Task task = taskWith(Map.of("orderId", "ORD-200", "shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SHIP-001", result.getOutputData().get("shipmentId"));
        assertEquals("ORD-200", result.getOutputData().get("orderId"));
        assertEquals("failed", result.getOutputData().get("shipStatus"));
    }

    @Test
    void shipOrder_shouldFailStringTrue() {
        ShipOrderWorker worker = new ShipOrderWorker();
        Task task = taskWith(Map.of("orderId", "ORD-300", "shouldFail", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("failed", result.getOutputData().get("shipStatus"));
    }

    @Test
    void shipOrder_shouldFailStringFalse() {
        ShipOrderWorker worker = new ShipOrderWorker();
        Task task = taskWith(Map.of("orderId", "ORD-400", "shouldFail", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipped", result.getOutputData().get("shipStatus"));
    }

    @Test
    void shipOrder_missingShouldFail() {
        ShipOrderWorker worker = new ShipOrderWorker();
        Task task = taskWith(Map.of("orderId", "ORD-500"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("shipped", result.getOutputData().get("shipStatus"));
    }

    @Test
    void shipOrder_alwaysReturnsDeterministicShipmentId() {
        ShipOrderWorker worker = new ShipOrderWorker();

        Task taskSuccess = taskWith(Map.of("orderId", "ORD-A", "shouldFail", false));
        Task taskFail = taskWith(Map.of("orderId", "ORD-B", "shouldFail", true));

        assertEquals("SHIP-001", worker.execute(taskSuccess).getOutputData().get("shipmentId"));
        assertEquals("SHIP-001", worker.execute(taskFail).getOutputData().get("shipmentId"));
    }

    // ---- RefundPaymentWorker tests ----

    @Test
    void refundPayment_taskDefName() {
        assertEquals("spi_refund_payment", new RefundPaymentWorker().getTaskDefName());
    }

    @Test
    void refundPayment_happyPath() {
        RefundPaymentWorker worker = new RefundPaymentWorker();
        Task task = taskWith(Map.of("paymentId", "PAY-001", "orderId", "ORD-200"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PAY-001", result.getOutputData().get("paymentId"));
        assertEquals("ORD-200", result.getOutputData().get("orderId"));
        assertEquals("refunded", result.getOutputData().get("refundStatus"));
    }

    @Test
    void refundPayment_missingInputs() {
        RefundPaymentWorker worker = new RefundPaymentWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("paymentId"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("refunded", result.getOutputData().get("refundStatus"));
    }

    // ---- ReleaseInventoryWorker tests ----

    @Test
    void releaseInventory_taskDefName() {
        assertEquals("spi_release_inventory", new ReleaseInventoryWorker().getTaskDefName());
    }

    @Test
    void releaseInventory_happyPath() {
        ReleaseInventoryWorker worker = new ReleaseInventoryWorker();
        Task task = taskWith(Map.of("reservationId", "INV-001", "orderId", "ORD-200"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("INV-001", result.getOutputData().get("reservationId"));
        assertEquals("ORD-200", result.getOutputData().get("orderId"));
        assertEquals("released", result.getOutputData().get("inventoryStatus"));
    }

    @Test
    void releaseInventory_missingInputs() {
        ReleaseInventoryWorker worker = new ReleaseInventoryWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("reservationId"));
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("released", result.getOutputData().get("inventoryStatus"));
    }

    // ---- Helper ----

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
